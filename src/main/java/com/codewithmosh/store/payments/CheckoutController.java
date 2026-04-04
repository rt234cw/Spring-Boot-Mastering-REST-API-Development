package com.codewithmosh.store.payments;

import com.codewithmosh.store.common.ErrorDto;
import com.codewithmosh.store.carts.CartEmptyException;
import com.codewithmosh.store.carts.CartNotFoundException;
import com.codewithmosh.store.orders.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;


    @PostMapping
    public CheckoutResponse checkout(
            @Valid @RequestBody CheckOutRequest request
            ) {
            return checkoutService.checkout(request);
    }

//    @PostMapping("/webhook")
//    public void handleWebhook(
//            @RequestHeader Map<String,String> headers,
//    @RequestBody String payload
//    ){
//        checkoutService.handleWebhookEvent(new WebhookRequest(headers,payload));
//
//    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(HttpServletRequest request) throws IOException {
        // 手動讀取原始 Body，這對 Stripe 簽名驗證最安全
        String payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        // 獲取所有 Header 並轉為 Map
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            headers.put(key, request.getHeader(key));
        }

        checkoutService.handleWebhookEvent(new WebhookRequest(headers, payload));
        return ResponseEntity.ok().build();
    }


    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<?> handlePaymentException(){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto("Error creating a checkout session"));
    }


    @ExceptionHandler({CartNotFoundException.class, CartEmptyException.class})
    public ResponseEntity<ErrorDto> handleException(Exception exception) {

        return ResponseEntity.badRequest().body(new ErrorDto(exception.getMessage()));

    }
}
