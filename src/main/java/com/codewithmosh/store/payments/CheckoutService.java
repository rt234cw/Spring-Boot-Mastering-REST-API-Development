package com.codewithmosh.store.payments;


import com.codewithmosh.store.orders.Order;
import com.codewithmosh.store.carts.CartEmptyException;
import com.codewithmosh.store.carts.CartNotFoundException;
import com.codewithmosh.store.carts.CartRepository;
import com.codewithmosh.store.orders.OrderRepository;
import com.codewithmosh.store.auth.AuthService;
import com.codewithmosh.store.carts.CartService;
import lombok.RequiredArgsConstructor;
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
       paymentGateway.parseWebhookRequest(webhookRequest)
               .ifPresent(paymentRequest -> {
                   var order = orderRepository.findById(paymentRequest.getOrderId()).orElseThrow();
                   order.setStatus(paymentRequest.getPaymentStatus());
                   orderRepository.save(order);
                       });


    }

}
