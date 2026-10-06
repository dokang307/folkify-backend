# Folkify Backend

Backend API cho ứng dụng học nhạc cụ dân tộc Việt Nam **Folkify**, xây dựng bằng Spring Boot 3.4.1 + PostgreSQL.

---

## Tech Stack

| Layer     | Công nghệ                                           |
| --------- | --------------------------------------------------- |
| Framework | Spring Boot 3.4.1, Spring Security, Spring Data JPA |
| Database  | PostgreSQL + Flyway (migration)                     |
| Auth      | JWT (JJWT 0.12.6) + OAuth2 (Google, Apple)          |
| Payment   | PayOS SDK 2.0.1                                     |
| Storage   | Cloudflare R2 (S3-compatible API)                   |
| API Docs  | Springdoc OpenAPI 3 / Swagger UI                    |
| Mail      | Spring Mail (SMTP)                                  |
| ID        | UUID v7 (time-ordered)                              |

---

## Cấu trúc module

```
com.folkify
├── auth           # Xác thực & quản lý người dùng
├── payment        # Thanh toán & quản lý gói
├── instrument     # Nhạc cụ, bài học, bài nhạc
├── blog           # Bài viết / tin tức
├── progress       # Tracking học tập & thành tích
├── admin          # Quản trị hệ thống
├── user           # Thông tin & cài đặt tài khoản
├── storage        # Upload file lên Cloudflare R2
├── config         # Cấu hình Security, Swagger, R2, App
├── security       # JWT Filter, Entry Point, Access Handler
├── exception      # GlobalExceptionHandler
├── common         # ApiResponse, ErrorCode, ApiException
└── infrastructure # BaseEntity (UUID v7 + timestamps)
```

---

## Logic hoạt động

### 1. Authentication

Mỗi request đi qua `JwtAuthFilter` trước khi chạm đến controller:

- Đọc header `Authorization: Bearer <token>`
- Gọi `JwtService.isTokenValid()` để xác thực chữ ký và thời hạn
- Extract `userId` từ claims → tìm `User` trong DB → gắn vào `SecurityContext`

**Đăng ký / Đăng nhập thông thường:**

1. `POST /api/auth/register` → lưu user với mật khẩu BCrypt hash
2. `POST /api/auth/login` → xác thực qua `AuthenticationManager` → trả `accessToken` (15 phút) + `refreshToken` (7 ngày)
3. `POST /api/auth/refresh-token` → kiểm tra refresh token chưa bị revoke, chưa hết hạn → cấp accessToken mới
4. `POST /api/auth/logout` → revoke refresh token

**Đăng nhập OAuth2:**

- Google: verify `idToken` qua Google API (`https://oauth2.googleapis.com/tokeninfo`) → lấy email → tìm hoặc tạo user
- Apple: fetch public key từ `https://appleid.apple.com/auth/keys` → verify JWT bằng RSA → lấy sub + email

**Reset mật khẩu:**

1. `POST /api/auth/forgot-password` → tạo `PasswordResetToken` (1 giờ) → gửi email chứa link
2. `GET /api/auth/reset-password/open?token=...` → trả trang HTML tự redirect sang deep link `folkify:///reset-password?token=...`
3. App nhận deep link → `POST /api/auth/reset-password` → xác thực token → cập nhật mật khẩu

---

### 2. Phân quyền

User có 2 thuộc tính kiểm soát quyền:

- **Role**: `USER` (mặc định) hoặc `ADMIN`
- **Plan**: `FREE` → `BASIC` → `PRO`

`SecurityConfig` phân loại endpoint:

- Public (không cần token): `POST /api/auth/**`, `GET /api/instruments/**`, `GET /api/blog/**`, webhook payment, Swagger UI
- Authenticated: tất cả endpoint còn lại
- Admin: dùng `@PreAuthorize("hasRole('ADMIN')")` ở method level

---

### 3. Nội dung học (Instrument / Lesson / Song)

**Instrument** là đối tượng trung tâm. Mỗi nhạc cụ có:

- Thông tin mô tả: tên, vùng miền, chất liệu, âm vực, độ khó, v.v.
- Danh sách `Lesson` (bài học) và `Song` (bài nhạc)

**Lesson** gồm:

- `steps`: mảng các bước hướng dẫn
- `tips`: mảng ghi chú / mẹo
- `xp`: điểm kinh nghiệm khi hoàn thành
- `youtubeUrl`: video demo
- `orderIndex`: thứ tự hiển thị

Tất cả endpoint instrument đều public (không cần đăng nhập).

---

### 4. Tracking tiến độ học (Progress)

- Khi user hoàn thành bài học → ghi `UserLessonProgress` (user_id + lesson_id + completedAt)
- `DailyActivity` ghi lại xp kiếm được + thời gian học mỗi ngày
- `Achievement` và `UserAchievement` quản lý hệ thống thành tích

---

### 5. Thanh toán (Payment)

Sử dụng cổng thanh toán **PayOS** (QR chuyển khoản ngân hàng Việt Nam).

**Ba gói:**
| Gói | Giá mặc định |
|---|---|
| FREE | Miễn phí |
| BASIC | 49.000 VND / 30 ngày |
| PRO | 99.000 VND / 30 ngày |

**Luồng thanh toán:**

```
App                    Backend                   PayOS
 │                        │                         │
 │  POST /checkout         │                         │
 │─────────────────────►  │                         │
 │                        │── tạo PaymentTransaction(PENDING)
 │                        │── gọi PayOS API ─────►  │
 │                        │◄── trả payUrl + orderId ─│
 │◄── {payUrl, orderId} ──│                         │
 │                        │                         │
 │  Mở WebView payUrl      │                         │
 │──────────────────────────────────────────────►   │
 │  User quét QR / chuyển khoản                     │
 │◄─────────────────── redirect /api/payments/result│
 │                        │                         │
 │  (Backend nhận webhook)│◄── POST /webhooks/payos ─│
 │                        │── verify checksum        │
 │                        │── cập nhật Transaction(SUCCESS)
 │                        │── nâng user.plan + set planExpiresAt
 │                        │                         │
 │  GET /status/{orderId} │                         │
 │─────────────────────►  │                         │
 │◄── {status: SUCCESS} ──│                         │
```

**Webhook processing:**

- PayOS gọi `POST /api/payments/webhooks/payos` (public endpoint, không cần JWT)
- Backend xác thực chữ ký (checksum key) → match `transferContent` với `gatewayReferenceId`
- Dùng `PESSIMISTIC_WRITE` lock để chặn xử lý song song cùng 1 order
- Cập nhật `user.plan` và `user.planExpiresAt = now + planDurationDays`

**Hết hạn gói:**

- `PlanExpiryJob` chạy mỗi 15 phút, tìm user có `plan != FREE` và `planExpiresAt < now` → hạ về `FREE`
- `PaymentTimeoutJob` cancel giao dịch `PENDING` quá thời gian

---

### 6. Blog

CRUD bài viết (admin quản lý, public đọc):

- `GET /api/blog` → danh sách bài đã published, sắp xếp theo `publishedAt` mới nhất
- `GET /api/blog/{slug}` → chi tiết bài viết

---

### 7. Admin

Endpoint bảo vệ bằng `ROLE_ADMIN`:

- Thống kê hệ thống
- Quản lý user (xem, xóa)
- CRUD nhạc cụ, bài học, bài nhạc, sheet nhạc, blog

---

### 8. Storage (Cloudflare R2)

`StorageController` cho phép upload file (ảnh, sheet nhạc) lên Cloudflare R2 thông qua AWS S3 SDK. Config qua `R2Config` với `account-id`, `access-key-id`, `secret-access-key`, `bucket-name`.

---

### 9. Error Handling

`GlobalExceptionHandler` xử lý tập trung, trả về `ApiResponse` với:

| Range     | Loại                          |
| --------- | ----------------------------- |
| 1001–1099 | Auth errors                   |
| 1100–1199 | Instrument / Lesson errors    |
| 1200–1299 | User errors                   |
| 1300–1399 | Payment errors                |
| 4000–4003 | Validation / Not found / Auth |
| 5000+     | Server errors                 |

---

## Cấu hình môi trường (.env)

Xem file `.env.example` để biết các biến cần thiết. Quan trọng nhất:

```
DB_URL, DB_USERNAME, DB_PASSWORD
JWT_SECRET
PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY
PAYOS_RETURN_URL, PAYOS_CANCEL_URL
PAYOS_PRICE_BASIC, PAYOS_PRICE_PRO
R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY
MAIL_USERNAME, MAIL_PASSWORD
```

---

## Chạy local

```bash
# Yêu cầu: Java 21, PostgreSQL, Maven
cp .env.example .env  # điền thông tin cần thiết
./mvnw spring-boot:run

# Hoặc dùng Docker
docker compose up
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Database Migration

Flyway tự chạy migration khi khởi động. Script nằm ở `src/main/resources/db/migration/`.

---

## Thay đổi đường dẫn thanh toán & Truy xuất transaction

Xem phần bên dưới trong tài liệu này.

---

# Hướng dẫn thay đổi & mở rộng Payment

## Thay đổi Return URL / Cancel URL cho gói BASIC và PRO

Hiện tại, `returnUrl` và `cancelUrl` là **dùng chung cho tất cả gói**. Chúng được cấu hình trong `application.yml`:

```yaml
payos:
  return-url: ${PAYOS_RETURN_URL:http://localhost:8080/api/payments/result}
  cancel-url: ${PAYOS_CANCEL_URL:http://localhost:8080/api/payments/result}
```

Và được đọc vào `PayOsProperties.returnUrl` / `PayOsProperties.cancelUrl`.

### Nếu muốn URL khác nhau theo từng gói

**Cần thay đổi ở 3 chỗ:**

1. **`PayOsProperties.java`** — Thêm 2 map:

   ```
   Map<Plan, String> returnUrls   // VD: BASIC -> "/api/payments/result/basic"
   Map<Plan, String> cancelUrls   // VD: PRO   -> "/api/payments/result/pro"
   ```

2. **`application.yml`** — Thêm các key tương ứng:

   ```yaml
   payos:
     return-urls:
       BASIC: ${PAYOS_RETURN_URL_BASIC:http://localhost:8080/api/payments/result?plan=basic}
       PRO: ${PAYOS_RETURN_URL_PRO:http://localhost:8080/api/payments/result?plan=pro}
   ```

3. **`CheckoutServiceImpl.java`** (impl của `CheckoutService`) — Khi build PayOS order request, lấy URL theo `request.plan()` thay vì lấy chung 1 URL.

**Lưu ý:** Endpoint `GET /api/payments/result` trong `CheckoutController` hiện đang làm nhiệm vụ nhận redirect từ PayOS rồi forward sang deep link `folkify://payment/result`. Nếu tách URL theo gói, bạn có thể thêm query param (vd `?plan=BASIC`) để app biết gói nào vừa thanh toán, hoặc tạo thêm endpoint riêng như `/api/payments/result/basic`.

---

## Truy xuất transaction khi người dùng thanh toán nâng cấp gói

Entity `PaymentTransaction` đã lưu đầy đủ thông tin:

| Field                 | Mô tả                                      |
| --------------------- | ------------------------------------------ |
| `user`                | User đã thanh toán                         |
| `targetPlan`          | Gói muốn mua (BASIC / PRO)                 |
| `gatewayReferenceId`  | orderId gửi lên PayOS (unique)             |
| `bankTransactionCode` | Mã giao dịch ngân hàng (do webhook trả về) |
| `amount`              | Số tiền (VND)                              |
| `transferContent`     | Nội dung chuyển khoản                      |
| `transactionDate`     | Thời điểm tạo giao dịch                    |
| `status`              | PENDING / SUCCESS / CANCELLED / FAILED     |

`PaymentTransactionRepository` đã có sẵn:

- `findByGatewayReferenceId(orderId)` — tìm theo order
- `findByStatusAndCreatedAtBefore(status, time)` — timeout job
- `findLastPurchaseDatePerUser(status)` — lần mua gần nhất của từng user

### Để thêm endpoint truy xuất transaction cho user / admin, cần:

1. **Thêm query vào `PaymentTransactionRepository`**:
   - `findByUserOrderByTransactionDateDesc(User user)` — lịch sử của 1 user
   - `findByUserAndStatus(User user, TransactionStatus status)` — lọc theo trạng thái
   - Hỗ trợ phân trang: dùng `Pageable` param

2. **Tạo DTO response** (ví dụ `TransactionHistoryResponse`) với các field cần hiển thị, tránh expose trực tiếp entity.

3. **Thêm service method** vào `CheckoutService` (hoặc tạo `TransactionService` riêng):
   - `getMyTransactions(User user, Pageable pageable)` cho user tự xem
   - `getAllTransactions(Pageable pageable)` cho admin

4. **Thêm endpoint**:
   - `GET /api/payments/transactions` — user xem lịch sử mua gói của mình (cần JWT)
   - `GET /api/admin/transactions` — admin xem toàn bộ (cần `ROLE_ADMIN`)

5. **`SecurityConfig`** không cần sửa vì các endpoint `/api/payments/**` (ngoài webhook) đã yêu cầu authenticated, và `/api/admin/**` đã có `@PreAuthorize` ở controller.
