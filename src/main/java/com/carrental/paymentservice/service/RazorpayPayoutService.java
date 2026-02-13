package com.carrental.paymentservice.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Slf4j
public class RazorpayPayoutService {

    @Value("${razorpay.keyId}") // Changed from razorpay.key.id
    private String keyId;

    @Value("${razorpay.keySecret}") // Changed from razorpay.key.secret
    private String keySecret;

    @Value("${razorpay.x.account.number:23232323232323}") // Added a default value
    private String rzpXAccountNumber;;

    public String createPayout(String name, String email, String accountNumber, String ifsc, BigDecimal amount) throws RazorpayException {
        RazorpayClient rzp = new RazorpayClient(keyId, keySecret);

        // 1. Create a Contact for the Owner
        JSONObject contactRequest = new JSONObject();
        contactRequest.put("name", name);
        contactRequest.put("email", email);
        contactRequest.put("type", "vendor");
        contactRequest.put("reference_id", email);

        // In real use, you'd check if contact already exists.
        // For test mode, we'll create or use a placeholder.

        // 2. Create Fund Account (The Bank Account)
        JSONObject fundAccountRequest = new JSONObject();
        fundAccountRequest.put("account_type", "bank_account");

        JSONObject bankAccount = new JSONObject();
        bankAccount.put("name", name);
        bankAccount.put("ifsc", ifsc);
        bankAccount.put("account_number", accountNumber);
        fundAccountRequest.put("bank_account", bankAccount);

        // 3. Create the Payout
        JSONObject payoutRequest = new JSONObject();
        payoutRequest.put("account_number", rzpXAccountNumber);
        payoutRequest.put("amount", amount.multiply(new BigDecimal(100)).intValue()); // Convert to Paise
        payoutRequest.put("currency", "INR");
        payoutRequest.put("mode", "IMPS"); // Instant transfer
        payoutRequest.put("purpose", "payout");

        log.info("Simulating RazorpayX Payout for: {} amount: {}", email, amount);

        // Note: In TEST MODE, the Payout will stay in 'processed' state.
        return "payout_test_id_" + System.currentTimeMillis();
    }
}