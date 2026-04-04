package com.codewithmosh.store.payments;


import com.codewithmosh.store.orders.Order;
import com.codewithmosh.store.carts.CartEmptyException;
import com.codewithmosh.store.carts.CartNotFoundException;
import com.codewithmosh.store.carts.CartRepository;
import com.codewithmosh.store.orders.OrderRepository;
import com.codewithmosh.store.auth.AuthService;
import com.codewithmosh.store.carts.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final AuthService authService;
    private final PaymentGateway paymentGateway;

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);




    @Transactional
    public CheckoutResponse checkout(
            CheckOutRequest request
    )  {
        var cart = cartRepository.getCartWithItems(request.getCartId()).orElse(null);
        if (cart == null ) {
            throw new CartNotFoundException();
        }

        if (cart.isCartEmpty()){
            throw new CartEmptyException();
        }

        var order = Order.fromCart(cart,authService.getCurrentUser());
        orderRepository.save(order);

        try {
            var session = paymentGateway.createCheckoutSession(order);
            cartService.clearCart(cart.getId());
            return new CheckoutResponse(order.getId(),session.getCheckoutUrl());
        } catch (PaymentException e) {
            System.out.println(e.getMessage());
            orderRepository.delete(order);
            throw e;
        }




    }

    public void handleWebhookEvent(WebhookRequest webhookRequest) {
        try {
            paymentGateway.parseWebhookRequest(webhookRequest)
                    .ifPresent(paymentRequest -> {

                        Long orderId = paymentRequest.getOrderId();
                        log.info("接收到 Webhook，準備更新訂單狀態。Order ID: {}", orderId);

                        // 改用 ifPresentOrElse 或捕獲錯誤，不要讓它默默拋出 500
                        var order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Webhook 錯誤：在資料庫中找不到 Order ID = " + orderId));

                        order.setStatus(paymentRequest.getPaymentStatus());
                        orderRepository.save(order);

                        log.info("訂單 {} 狀態已成功更新為 {}", orderId, paymentRequest.getPaymentStatus());
                    });
        } catch (Exception e) {
            // 將詳細的錯誤原因印在 Railway 的 Logs 裡面！
            log.error("處理 Webhook 時發生嚴重的內部錯誤: ", e);
            // 若需要讓 Stripe 知道失敗並重試，可以考慮重新拋出異常，或者回傳 500
            throw new RuntimeException("Webhook 處理失敗", e);
        }
    }

}
