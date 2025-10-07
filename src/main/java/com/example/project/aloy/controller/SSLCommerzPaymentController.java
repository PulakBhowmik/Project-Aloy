package com.example.project.aloy.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.project.aloy.repository.PaymentRepository;
import com.example.project.aloy.repository.ApartmentRepository;
import com.example.project.aloy.model.Payment;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.math.BigDecimal;


@RestController
@RequestMapping("/api/payments")
public class SSLCommerzPaymentController {

    @Value("${sslcommerz.store.id}")
    private String storeId;

    @Value("${sslcommerz.store.password}")
    private String storePassword;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @PostMapping("/initiate")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> paymentRequest) {
    System.out.println("[DEBUG] Payment initiation request: " + paymentRequest);
        // Log credentials for debugging
        System.out.println("SSLCommerz storeId: " + storeId);
        System.out.println("SSLCommerz storePassword: " + storePassword);
        
        // Prepare payment data
        Map<String, Object> payload = new HashMap<>();
        
        // Store credentials
        payload.put("store_id", storeId);
        payload.put("store_passwd", storePassword);
        
        // Transaction details
        Double amount = Double.parseDouble(paymentRequest.get("amount").toString());
        payload.put("total_amount", String.format("%.2f", amount));
        payload.put("currency", "BDT");

    // If apartmentId provided, check availability and create a PENDING Payment so we get a stable tran_id
    String debugTranId = null;
        Long apartmentId = null;
        System.out.println("[DEBUG] Checking apartmentId in request: " + paymentRequest.get("apartmentId"));
        if (paymentRequest.get("apartmentId") != null && !String.valueOf(paymentRequest.get("apartmentId")).isEmpty()) {
            try {
                apartmentId = Long.parseLong(String.valueOf(paymentRequest.get("apartmentId")));
                System.out.println("[DEBUG] Parsed apartmentId: " + apartmentId);
                // lock the apartment row to prevent race conditions
                java.util.Optional<com.example.project.aloy.model.Apartment> locked = apartmentRepository.findByIdForUpdate(apartmentId);
                if (locked.isPresent()) {
                    com.example.project.aloy.model.Apartment ap = locked.get();
                    // Check if apartment is already booked or status is RENTED
                    if (ap.isBooked() || "RENTED".equalsIgnoreCase(ap.getStatus())) {
                        System.out.println("[WARNING] Apartment " + apartmentId + " is already booked (booked=" + ap.isBooked() + ", status=" + ap.getStatus() + ")");
                        return ResponseEntity.status(409).body(Collections.singletonMap("error", "This apartment is already booked. Please choose another one."));
                    }

                    // Try to find existing payment for this apartment
                    Payment existing = paymentRepository.findByApartmentId(apartmentId).orElse(null);
                    if (existing != null) {
                        if ("COMPLETED".equalsIgnoreCase(existing.getStatus())) {
                            System.out.println("[WARNING] Apartment " + apartmentId + " already has a completed payment (ID: " + existing.getPaymentId() + ")");
                            return ResponseEntity.status(409).body(Collections.singletonMap("error", "This apartment is already paid for. Please choose another one."));
                        }
                        // reuse existing pending payment
                        debugTranId = existing.getTransactionId() != null ? existing.getTransactionId() : "PAY" + existing.getPaymentId();
                        payload.put("tran_id", debugTranId);
                    } else {
                        // create a new pending payment
                        Payment p = new Payment();
                        p.setAmount(BigDecimal.valueOf(amount));
                        p.setPaymentMethod("SSLCommerz");
                        p.setApartmentId(apartmentId);
                        // Extract tenantId from request
                        if (paymentRequest.get("tenantId") != null) {
                            try {
                                Long tenantId = Long.parseLong(String.valueOf(paymentRequest.get("tenantId")));
                                p.setTenantId(tenantId);
                            } catch (NumberFormatException ignored) {}
                        }
                        p.setStatus("PENDING");
                        p.setCreatedAt(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        Payment saved = paymentRepository.save(p);
                        String tran = "PAY" + saved.getPaymentId();
                        saved.setTransactionId(tran);
                        paymentRepository.save(saved);
                        debugTranId = tran;
                        payload.put("tran_id", tran);
                        System.out.println("[DEBUG] Created and saved payment record with ID: " + saved.getPaymentId() + ", transactionId: " + tran);
                    }
                } else {
                    return ResponseEntity.status(404).body("Apartment not found");
                }
            } catch (NumberFormatException nfe) {
                System.out.println("[ERROR] Failed to parse apartmentId: " + paymentRequest.get("apartmentId"));
                return ResponseEntity.badRequest().body("Invalid apartmentId");
            }
        } else {
            // No apartment context: generate a generic tran_id and create payment record
            System.out.println("[DEBUG] No apartmentId provided, creating generic payment. Request keys: " + paymentRequest.keySet());
            debugTranId = "TXN" + System.currentTimeMillis();
            payload.put("tran_id", debugTranId);
            
            // Create a PENDING payment record for non-apartment payments too
            Payment p = new Payment();
            p.setAmount(BigDecimal.valueOf(amount));
            p.setPaymentMethod("SSLCommerz");
            p.setTransactionId(debugTranId);
            // Extract tenantId from request
            if (paymentRequest.get("tenantId") != null) {
                try {
                    Long tenantId = Long.parseLong(String.valueOf(paymentRequest.get("tenantId")));
                    p.setTenantId(tenantId);
                } catch (NumberFormatException ignored) {}
            }
            p.setStatus("PENDING");
            p.setCreatedAt(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            Payment saved = paymentRepository.save(p);
            System.out.println("[DEBUG] Created generic payment record with ID: " + saved.getPaymentId() + ", transactionId: " + debugTranId);
    System.out.println("[DEBUG] Payment initiation transactionId: " + debugTranId);
        }
        
        // Use the correct receipt download URL for success
        // Use debugTranId directly to avoid any issues with payload extraction
        payload.put("success_url", "http://localhost:8080/payment-success/download?tran_id=" + debugTranId);
        payload.put("fail_url", "http://localhost:8080/payment-fail");
        payload.put("cancel_url", "http://localhost:8080/payment-cancel");
        
        // Pass apartmentId and tenantId via custom fields for callback
        if (apartmentId != null) {
            payload.put("value_a", apartmentId.toString());
        }
        if (paymentRequest.get("tenantId") != null) {
            payload.put("value_b", paymentRequest.get("tenantId").toString());
        }
        
        // Customer details
        payload.put("cus_name", paymentRequest.getOrDefault("name", "Test User"));
        payload.put("cus_email", paymentRequest.getOrDefault("email", "test@example.com"));
        payload.put("cus_phone", paymentRequest.getOrDefault("phone", "01700000000"));
        payload.put("cus_add1", "Dhaka");
        payload.put("cus_add2", "Bangladesh");
        payload.put("cus_city", "Dhaka");
        payload.put("cus_state", "Dhaka");
        payload.put("cus_postcode", "1000");
        payload.put("cus_country", "Bangladesh");
        
        // Shipping details (required even for non-shipping products)
        payload.put("shipping_method", "NO");
        payload.put("ship_name", paymentRequest.getOrDefault("name", "Test User"));
        payload.put("ship_add1", "Dhaka");
        payload.put("ship_add2", "Bangladesh");
        payload.put("ship_city", "Dhaka");
        payload.put("ship_state", "Dhaka");
        payload.put("ship_postcode", "1000");
        payload.put("ship_country", "Bangladesh");
        
        // Product details
        payload.put("product_name", "Apartment Rent");
        payload.put("product_category", "Rental");
        payload.put("product_profile", "general");
        
    // Optional reference values: pass apartmentId and tenantId so we can associate after callback
    Object apartmentIdObj = paymentRequest.get("apartmentId");
    Object tenantIdObj = paymentRequest.get("tenantId");
    payload.put("value_a", apartmentIdObj == null ? "" : String.valueOf(apartmentIdObj));
    payload.put("value_b", tenantIdObj == null ? "" : String.valueOf(tenantIdObj));
        payload.put("value_c", "ref003");
        payload.put("value_d", "ref004");
    payload.put("emi_option", 0);

        // Log the complete payload
        System.out.println("\nSending payload to SSLCommerz:");
        payload.forEach((key, value) -> System.out.println(key + ": " + value));
        
        // Call SSLCommerz API (expects application/x-www-form-urlencoded)
        String apiUrl = "https://sandbox.sslcommerz.com/gwprocess/v3/api.php";
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            payload.forEach((k, v) -> form.add(k, v == null ? "" : String.valueOf(v)));

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

            // Use postForEntity to capture raw HTTP status and body
            ResponseEntity<String> respEntity = restTemplate.postForEntity(apiUrl, request, String.class);
            int statusCode = respEntity.getStatusCode().value();
            String body = respEntity.getBody();

            System.out.println("\nSSLCommerz HTTP status: " + statusCode);
            System.out.println("SSLCommerz raw body: " + body);

            if (body == null || body.isEmpty()) {
                return ResponseEntity.status(502).body("Empty response from payment gateway (HTTP " + statusCode + ")");
            }

            // Try to parse JSON body into a Map
            Map<String, Object> response;
            try {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = mapper.readValue(body, Map.class);
                response = parsed == null ? new HashMap<>() : parsed;
            } catch (Exception jsonEx) {
                System.out.println("Failed to parse JSON response: " + jsonEx.getMessage());
                // Return raw body for debugging
                return ResponseEntity.status(502).body("Non-JSON response from gateway: " + body);
            }

            System.out.println("\nParsed SSLCommerz Response:");
            response.forEach((key, value) -> System.out.println(key + ": " + value));

            Object status = response.get("status");
            Object gateway = response.get("GatewayPageURL");

            if (status != null && "FAILED".equalsIgnoreCase(String.valueOf(status))) {
                // Return parsed response to client for inspection
                return ResponseEntity.status(400).body(response);
            }

            if (gateway == null || String.valueOf(gateway).isEmpty()) {
                return ResponseEntity.status(502).body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // Print full stack trace for debugging
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // SSLCommerz will redirect to this URL after payment (GET) and may POST server-to-server (IPN)
            // Note: /payment-success is handled in PaymentResultController to avoid duplicate mappings and
            // to provide a single place that returns a PDF receipt for GET (view) and POST (gateway callbacks).
}