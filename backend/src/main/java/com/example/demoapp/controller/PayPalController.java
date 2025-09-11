// src/main/java/com/example/demoapp/controller/PayPalController.java
package com.example.demoapp.controller;
// Add this import
import com.example.demoapp.util.JwtUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.demoapp.model.Purchase;
import com.example.demoapp.model.Software;
import com.example.demoapp.model.User;
import com.example.demoapp.repository.PurchaseRepository;
import com.example.demoapp.repository.SoftwareRepository;
import com.example.demoapp.repository.UserRepository;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://192.168.1.33:3000", allowCredentials = "true")
@RequestMapping("/api/payment")
public class PayPalController {

    @Autowired
    private APIContext apiContext;

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;
    
   @Autowired
   private JwtUtil jwtUtil;
    /**
     * Create PayPal payment
     */
    @PostMapping("/create-payment")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        try {
            String username = getUsernameFromRequest(request);
            if (username == null) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            Long softwareId = Long.parseLong(data.get("softwareId").toString());
            Optional<Software> softwareOpt = softwareRepository.findById(softwareId);
            
            if (softwareOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Software not found");
            }

            Software software = softwareOpt.get();

            // Set payment details
            Amount amount = new Amount();
            amount.setCurrency("USD");
            amount.setTotal(String.format("%.2f", software.getPrice()));

            Transaction transaction = new Transaction();
            transaction.setDescription("Purchase: " + software.getTitle());
            transaction.setAmount(amount);

            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);

            Payer payer = new Payer();
            payer.setPaymentMethod("paypal");

            Payment payment = new Payment();
            payment.setIntent("sale");
            payment.setPayer(payer);
            payment.setTransactions(transactions);

            RedirectUrls redirectUrls = new RedirectUrls();
            redirectUrls.setCancelUrl("http://192.168.1.40:3000/payment-cancel");
            redirectUrls.setReturnUrl("http://192.168.1.40:3000/payment-success");
            payment.setRedirectUrls(redirectUrls);

            // Create payment
            Payment createdPayment = payment.create(apiContext);
            
            // Find approval URL
            String approvalUrl = null;
            for (Links link : createdPayment.getLinks()) {
                if (link.getRel().equals("approval_url")) {
                    approvalUrl = link.getHref();
                    break;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", createdPayment.getId());
            response.put("approvalUrl", approvalUrl);
            response.put("softwareId", softwareId);
            response.put("softwareTitle", software.getTitle());
            response.put("amount", software.getPrice());

            return ResponseEntity.ok(response);

        } catch (PayPalRESTException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating payment: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Execute PayPal payment after approval
     */
    @PostMapping("/execute-payment")
    public ResponseEntity<?> executePayment(@RequestBody Map<String, Object> data, HttpServletRequest request) {
        try {
            String username = getUsernameFromRequest(request);
            if (username == null) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            String paymentId = data.get("paymentId").toString();
            String payerId = data.get("payerId").toString();
            Long softwareId = Long.parseLong(data.get("softwareId").toString());

            PaymentExecution paymentExecution = new PaymentExecution();
            paymentExecution.setPayerId(payerId);

            Payment payment = new Payment();
            payment.setId(paymentId);

            // Execute payment
            Payment executedPayment = payment.execute(apiContext, paymentExecution);

            if (executedPayment.getState().equals("approved")) {
                // Payment successful - record purchase
                Optional<User> userOpt = userRepository.findByUsername(username);
                Optional<Software> softwareOpt = softwareRepository.findById(softwareId);
                
                if (userOpt.isEmpty() || softwareOpt.isEmpty()) {
                    return ResponseEntity.status(404).body("User or software not found");
                }

                // Check if already purchased
                if (purchaseRepository.existsByUserAndSoftwareId(userOpt.get(), softwareId)) {
                    return ResponseEntity.status(409).body("Software already purchased");
                }

                // Record purchase
                Purchase purchase = new Purchase();
                purchase.setUser(userOpt.get());
                purchase.setSoftware(softwareOpt.get());
                purchase.setPurchaseDate(LocalDateTime.now());
                purchase.setPricePaid(softwareOpt.get().getPrice());
                purchase.setPaymentId(paymentId);
                
                purchaseRepository.save(purchase);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Payment completed successfully");
                response.put("paymentId", paymentId);
                response.put("softwareId", softwareId);

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(400).body("Payment not approved");
            }

        } catch (PayPalRESTException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error executing payment: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }



// Add this method to your existing PayPalController class
@GetMapping("/purchases/check/{softwareId}")
public ResponseEntity<?> checkPurchase(@PathVariable Long softwareId, HttpServletRequest request) {
    try {
        String username = getUsernameFromRequest(request);
        if (username == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        boolean hasPurchased = purchaseRepository.existsByUserAndSoftwareId(userOpt.get(), softwareId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasPurchased", hasPurchased);
        
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Error checking purchase: " + e.getMessage());
    }
}
























    private String getUsernameFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Implement your JWT utility to extract username
                return jwtUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
