package com.codewithmosh.store.payments;


import com.codewithmosh.store.orders.Order;
import com.codewithmosh.store.orders.OrderItem;
import com.codewithmosh.store.orders.PaymentStatus;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class StripePaymentGateway implements PaymentGateway {

    @Value("${stripe.webhookSecretKey}")
    private String webhookSecretKey;

    @Value("${websiteUrl}")
    private String websiteUrl;

    @Override
    public CheckoutSession createCheckoutSession(Order order) {
        try {
            // create a checkout session
            var builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(websiteUrl + "/checkout-success?orderId=" + order.getId())
                    .setCancelUrl(websiteUrl + "/checkout-cancel.html")
                    .setPaymentIntentData(createPaymentIntent(order)); // 修正拼字

            order.getItems().forEach(item -> {
                var lineItem = createLineItem(item);
                builder.addLineItem(lineItem);
            });

            var session = Session.create(builder.build());
            return new CheckoutSession(session.getUrl());
        } catch (StripeException e) {
            System.out.println(e.getMessage());
            throw new PaymentException("Failed to create Stripe checkout session");
        }
    }

    private static SessionCreateParams.PaymentIntentData createPaymentIntent(Order order) {
        return SessionCreateParams.PaymentIntentData.builder()
                // 這裡存進去的是 "order_id"
                .putMetadata("order_id", order.getId().toString()).build();
    }

    @Override
    public Optional<PaymentResult> parseWebhookRequest(WebhookRequest webhookRequest) {
        try {
            var payload = webhookRequest.getPayload();
            var signature = webhookRequest.getHeaders().get("stripe-signature");
            // 驗證簽章並構造事件
            var event = Webhook.constructEvent(payload, signature, webhookSecretKey);

            return switch (event.getType()) {
                case "payment_intent.succeeded" ->
                    // 如果 extractOrderId 有找到 ID，就會轉換成 PaymentResult；如果沒找到，就會自動變成 Optional.empty()
                        extractOrderId(event).map(orderId -> new PaymentResult(orderId, PaymentStatus.PAID));

                case "payment_intent.payment_failed" ->
                        extractOrderId(event).map(orderId -> new PaymentResult(orderId, PaymentStatus.FAILED));

                default ->
                        Optional.empty();
            };

        } catch (SignatureVerificationException e) {
            throw new PaymentException("Invalid Stripe-Signature");
        } catch (Exception e) {
            // 加上一個廣泛的 Exception 捕捉，避免其他未知的解析錯誤導致系統崩潰
            System.err.println("Webhook 處理過程中發生非預期錯誤: " + e.getMessage());
            return Optional.empty();
        }
    }

    // 將回傳型態改為 Optional<Long>
    private Optional<Long> extractOrderId(Event event) {
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            PaymentIntent paymentIntent = (PaymentIntent) dataObjectDeserializer.getObject().orElse(null);

            if (paymentIntent == null || paymentIntent.getMetadata() == null) {
                System.out.println("警告: Webhook 事件中無法取得 PaymentIntent 或 Metadata 資訊。");
                return Optional.empty();
            }

            // 【重要修正】對齊上面的 putMetadata，這裡必須抓 "order_id" 而不是 "orderId"
            String orderIdStr = paymentIntent.getMetadata().get("order_id");

            if (orderIdStr == null || orderIdStr.isBlank()) {
                System.out.println("警告: 此 Webhook 事件的 Metadata 中沒有包含 order_id，已忽略。");
                return Optional.empty();
            }

            return Optional.of(Long.parseLong(orderIdStr));

        } catch (NumberFormatException e) {
            System.err.println("錯誤: Order ID 格式無法解析 (" + e.getMessage() + ")");
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("錯誤: 提取 Order ID 時發生異常 (" + e.getMessage() + ")");
            return Optional.empty();
        }
    }

    private SessionCreateParams.LineItem createLineItem(OrderItem item) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity(Long.valueOf(item.getQuantity()))
                .setPriceData(createPriceData(item))
                .build();
    }

    private SessionCreateParams.LineItem.PriceData createPriceData(OrderItem item) {
        return SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency("usd")
                .setUnitAmountDecimal(item.getUnitPrice().multiply(BigDecimal.valueOf(100)))
                .setProductData(createProductData(item))
                .build();
    }

    private SessionCreateParams.LineItem.PriceData.ProductData createProductData(OrderItem item) {
        return SessionCreateParams.LineItem.PriceData.ProductData.builder()
                .setName(item.getProduct().getName())
                .build();
    }
}