package com.codewithmosh.store.payments;


import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;



@Configuration
public class StripeConfig {
    @Value("${stripe.secretKey}")
    private String secretKey;



    @PostConstruct  // 用途：當stripe config bean建立後，呼叫這個constructor
    public void init() {
        Stripe.apiKey = secretKey;
    }

}
