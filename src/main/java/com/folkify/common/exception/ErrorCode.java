package com.folkify.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    SUCCESS(1000, "Thành công", HttpStatus.OK),

    // Auth (1001 - 1099)
    EMAIL_ALREADY_EXISTS(1001, "Email đã được sử dụng", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(1002, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(1003, "Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(1004, "Refresh token đã hết hạn hoặc bị thu hồi", HttpStatus.UNAUTHORIZED),
    GOOGLE_AUTH_FAILED(1005, "Xác thực Google thất bại, vui lòng thử lại", HttpStatus.UNAUTHORIZED),
    INVALID_RESET_TOKEN(1006, "Token đặt lại mật khẩu không hợp lệ hoặc đã được sử dụng", HttpStatus.BAD_REQUEST),
    APPLE_AUTH_FAILED(1008, "Xác thực Apple thất bại, vui lòng thử lại", HttpStatus.UNAUTHORIZED),
    RESET_TOKEN_EXPIRED(1007, "Token đặt lại mật khẩu đã hết hạn", HttpStatus.BAD_REQUEST),

    // Instrument (1100 - 1199)
    INSTRUMENT_NOT_FOUND(1100, "Không tìm thấy nhạc cụ", HttpStatus.NOT_FOUND),
    LESSON_NOT_FOUND(1101, "Không tìm thấy bài học", HttpStatus.NOT_FOUND),
    LESSON_ALREADY_COMPLETED(1102, "Bài học đã được hoàn thành trước đó", HttpStatus.CONFLICT),
    SONG_NOT_FOUND(1103, "Không tìm thấy bài hát", HttpStatus.NOT_FOUND),
    SHEET_NOT_FOUND(1104, "Không tìm thấy sheet nhạc", HttpStatus.NOT_FOUND),
    BLOG_POST_NOT_FOUND(1105, "Không tìm thấy bài viết", HttpStatus.NOT_FOUND),
    PLAN_REQUIRED(1106, "Bài học này cần nâng cấp gói để truy cập", HttpStatus.FORBIDDEN),

    // User (1200 - 1299)
    USER_NOT_FOUND(1200, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    WRONG_PASSWORD(1201, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),

    // Payment (1300 - 1399)
    INVALID_PLAN(1300, "Gói thanh toán không hợp lệ", HttpStatus.BAD_REQUEST),
    ALREADY_ON_PLAN(1301, "Bạn đang sử dụng gói này rồi", HttpStatus.CONFLICT),
    PAYMENT_NOT_FOUND(1302, "Không tìm thấy giao dịch thanh toán", HttpStatus.NOT_FOUND),
    PAYMENT_GATEWAY_ERROR(1303, "Lỗi kết nối cổng thanh toán", HttpStatus.BAD_GATEWAY),

    // AI scoring (1310 - 1319)
    AI_NOT_IN_PLAN(1310, "Gói hiện tại chưa hỗ trợ chấm điểm AI", HttpStatus.FORBIDDEN),
    AI_QUOTA_EXCEEDED(1311, "Bạn đã dùng hết lượt chấm điểm AI tháng này", HttpStatus.TOO_MANY_REQUESTS),
    AUDIO_INVALID(1312, "File ghi âm không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY),
    AI_SERVICE_UNAVAILABLE(1313, "Hệ thống chấm điểm đang bận, vui lòng thử lại sau", HttpStatus.SERVICE_UNAVAILABLE),
    REFERENCE_NOT_READY(1315, "Tác phẩm này chưa có bản mẫu để AI chấm điểm", HttpStatus.CONFLICT),

    // General (4000+)
    VALIDATION_ERROR(4000, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(4004, "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),
    UNAUTHORIZED(4001, "Vui lòng đăng nhập để tiếp tục", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(4003, "Không có quyền truy cập", HttpStatus.FORBIDDEN),

    // Server (5000+)
    UNEXPECTED_ERROR(5000, "Đã xảy ra lỗi, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getStatus() { return status; }
}
