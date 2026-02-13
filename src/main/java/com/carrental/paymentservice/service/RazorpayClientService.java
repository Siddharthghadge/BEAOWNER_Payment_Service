package com.carrental.paymentservice.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayClientService {

    private final RazorpayClient client;

    public RazorpayClientService(
            @Value("${razorpay.keyId}") String keyId,
            @Value("${razorpay.keySecret}") String keySecret
    ) throws Exception {
        this.client = new RazorpayClient(keyId, keySecret);
    }

    public Order createOrder(long amountInPaise, String currency, String receipt) throws Exception {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);  // amount in paise
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);
        orderRequest.put("payment_capture", 1);

        // Correct method for Razorpay SDK 1.4.6
        return client.orders.create(orderRequest);
    }
}
