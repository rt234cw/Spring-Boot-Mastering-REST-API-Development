package com.codewithmosh.store.payments;


import com.codewithmosh.store.orders.Order;
import com.codewithmosh.store.orders.OrderItem;
import com.codewithmosh.store.orders.PaymentStatus;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
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
        try {        //create a checkout session
            var builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(websiteUrl + "/checkout-success?orderId=" + order.getId())
                    .setCancelUrl(websiteUrl + "/checkout-cancel.html")
                            .setPaymentIntentData(createPaymentIndent(order));


            order.getItems().forEach(item -> {
                var lineItem = createLineItem(item);
                builder.addLineItem(lineItem);
            });

            var session = Session.create(builder.build());
            return new CheckoutSession(session.getUrl());
        } catch (StripeException e) {
            System.out.println(e.getMessage());
            throw new PaymentException();
        }
    }

    private static SessionCreateParams.PaymentIntentData createPaymentIndent(Order order) {
        return SessionCreateParams.PaymentIntentData.builder()
                .putMetadata("order_id", order.getId().toString()).build();
    }

    @Override
    public Optional<PaymentResult> parseWebhookRequest(WebhookRequest webhookRequest) {
        try {
            var payload = webhookRequest.getPayload();
            var signature = webhookRequest.getHeaders().get("Stripe-Signature");
            var event = Webhook.constructEvent(payload, signature, webhookSecretKey);

         return switch (event.getType()) {
                case "payment_intent.succeeded" ->
                         Optional.of(new PaymentResult(extractOrderId(event), PaymentStatus.PAID));

                case "payment_intent.payment_failed" ->
                     Optional.of(new PaymentResult(extractOrderId(event), PaymentStatus.FAILED));

                    default ->
                         Optional.empty();
            };

        } catch (SignatureVerificationException e) {
         throw new PaymentException("Invalid Stripe-Signature");
        }

    }

    private Long extractOrderId(Event event) {
        var stripeObject = event.getDataObjectDeserializer().getObject().orElseThrow(() -> new PaymentException("Could not deserialize Stripe event"));
        var paymentIntent = (PaymentIntent) stripeObject;
            return Long.valueOf(paymentIntent.getMetadata().get("order_id"));
        }

        private SessionCreateParams.LineItem createLineItem (OrderItem item){
            return SessionCreateParams.LineItem.builder()
                    .setQuantity(Long.valueOf(item.getQuantity()))
                    .setPriceData(createPriceData(item)
                    ).build();
        }

        private SessionCreateParams.LineItem.PriceData createPriceData (OrderItem item){
            return SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("usd")
                    .setUnitAmountDecimal(item.getUnitPrice().multiply(BigDecimal.valueOf(100)))
                    .setProductData(
                            createProductData(item)
                    ).build();
        }

        private SessionCreateParams.LineItem.PriceData.ProductData createProductData (OrderItem item){
            return SessionCreateParams.LineItem.PriceData.ProductData.builder()
                    .setName(item.getProduct().getName())
                    .build();
        }
    }
