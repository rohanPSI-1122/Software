package com.example.demoapp.controller;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://192.168.1.40:3000")
@RequestMapping("/api")
public class PaymentController {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @PostMapping("/create-checkout-session")
    public Map<String, String> createCheckoutSession(@RequestBody Map<String, String> data) throws Exception {
        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://192.168.1.40:3000/success.html")
                .setCancelUrl("http://192.168.1.40:3000/dashboard")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(data.get("priceId"))
                        .setQuantity(1L)
                        .build())
                .build();

        Session session = Session.create(params);
        Map<String, String> response = new HashMap<>();
        response.put("sessionId", session.getId());
        return response;
    }
}
