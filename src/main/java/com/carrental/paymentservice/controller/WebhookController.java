package com.carrental.paymentservice.controller;

import com.carrental.paymentservice.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final PaymentService paymentService;
    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleWebhook(@RequestHeader("X-Razorpay-Signature") String signature,
                                                @RequestBody String payload) throws Exception {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("Webhook processed");
    }
}
