package com.folkify.payment.service.impl;

import java.util.concurrent.atomic.AtomicLong;
import com.folkify.payment.enumType.CheckoutPlatform;
import com.folkify.auth.entity.Plan;
import com.folkify.auth.entity.User;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.payment.config.PayOsProperties;
import com.folkify.payment.dto.request.CheckoutRequest;
import com.folkify.payment.dto.response.CheckoutResponse;
import com.folkify.payment.dto.response.PaymentStatusResponse;
import com.folkify.payment.entity.PaymentTransaction;
import com.folkify.payment.enumType.TransactionStatus;
import com.folkify.payment.repository.PaymentTransactionRepository;
import com.folkify.payment.service.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutServiceImpl.class);

    private final PaymentTransactionRepository transactionRepo;
    private final PayOS payOS;
    private final PayOsProperties props;

    public CheckoutServiceImpl(PaymentTransactionRepository transactionRepo,
                               PayOS payOS,
                               PayOsProperties props) {
        this.transactionRepo = transactionRepo;
        this.payOS = payOS;
        this.props = props;
    }

    @Override
    public CheckoutResponse createCheckout(User currentUser, CheckoutRequest request) {
        Plan targetPlan = request.plan();

        if (targetPlan == null || targetPlan == Plan.FREE) {
            throw new ApiException(ErrorCode.INVALID_PLAN, "Gói không hợp lệ để thanh toán");
        }

        Long price = props.getPlanPrices().get(targetPlan);
        if (price == null || price <= 0) {
            throw new ApiException(ErrorCode.INVALID_PLAN,
                    "Chưa cấu hình giá cho gói " + targetPlan);
        }

        // Cho phép mua lại đúng gói đang dùng để GIA HẠN (cộng dồn thời hạn ở bước webhook).

        // orderCode: PayOS yêu cầu số nguyên dương, duy nhất theo merchant — dùng để đối soát webhook.
        long orderCode = generateOrderCode();
        // Nội dung đơn hàng — PayOS giới hạn tối đa 25 ký tự, để ngắn gọn không dấu.
        String description = "Folkify " + targetPlan.name();

        // Lưu giao dịch PENDING và commit ngay (method không @Transactional) trước khi gọi PayOS.
        PaymentTransaction txn = new PaymentTransaction();
        txn.setUser(currentUser);
        txn.setTargetPlan(targetPlan);
        txn.setGatewayReferenceId(String.valueOf(orderCode));
        txn.setAmount(BigDecimal.valueOf(price));
        txn.setTransferContent(description);
        txn.setTransactionDate(LocalDateTime.now());
        txn.setStatus(TransactionStatus.PENDING);
        transactionRepo.saveAndFlush(txn);

        boolean web = request.platformOrDefault() == CheckoutPlatform.WEB;
        try {
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name(description)
                    .quantity(1)
                    .price(price)
                    .build();

            CreatePaymentLinkRequest payload = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(price)
                    .description(description)
                    .item(item)
                    .returnUrl(web ? props.getWebReturnUrl() : props.getReturnUrl())
                    .cancelUrl(web ? props.getWebCancelUrl() : props.getCancelUrl())
                    .build();

            CreatePaymentLinkResponse data = payOS.paymentRequests().create(payload);
            log.info("Tạo link PayOS thành công | orderCode: {} | paymentLinkId: {}",
                    orderCode, data.getPaymentLinkId());

            return new CheckoutResponse(data.getCheckoutUrl(), String.valueOf(orderCode));
        } catch (Exception e) {
            log.error("Gọi PayOS tạo link thất bại | orderCode: {}", orderCode, e);
            throw new ApiException(ErrorCode.PAYMENT_GATEWAY_ERROR, "Lỗi kết nối cổng thanh toán");
        }
    }

    @Override
    public PaymentStatusResponse getStatus(User currentUser, String orderId) {
        PaymentTransaction txn = transactionRepo.findByGatewayReferenceId(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));

        if (txn.getUser() == null || !txn.getUser().getId().equals(currentUser.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        return new PaymentStatusResponse(orderId, txn.getStatus(), txn.getTargetPlan());
    }

    /** orderCode cấp gần nhất trong process — chặn 2 checkout cùng mili-giây lấy trùng số. */
    private static final AtomicLong LAST_ORDER_CODE = new AtomicLong();

    /** Sinh orderCode duy nhất (tăng đơn điệu theo mili-giây), tránh trùng với giao dịch đã lưu. */
    private long generateOrderCode() {
        long candidate = LAST_ORDER_CODE.updateAndGet(prev -> Math.max(prev + 1, System.currentTimeMillis()));
        while (transactionRepo.existsByGatewayReferenceId(String.valueOf(candidate))) {
            candidate++;
        }
        return candidate;
    }
}
