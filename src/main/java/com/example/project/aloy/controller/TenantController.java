package com.example.project.aloy.controller;

import com.example.project.aloy.model.Payment;
import com.example.project.aloy.model.Apartment;
import com.example.project.aloy.repository.PaymentRepository;
import com.example.project.aloy.repository.ApartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
@CrossOrigin(origins = "*")
public class TenantController {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ApartmentRepository apartmentRepository;

    /**
     * Check if a tenant has an active booking
     * Returns booking details if found
     */
    @GetMapping("/{userId}/booking-status")
    public ResponseEntity<?> getBookingStatus(@PathVariable Long userId) {
        try {
            // Find all payments for this tenant
            List<Payment> payments = paymentRepository.findByTenantId(userId);
            
            // Look for a COMPLETED payment (active booking)
            for (Payment payment : payments) {
                if ("COMPLETED".equalsIgnoreCase(payment.getStatus()) && payment.getApartmentId() != null) {
                    // Found active booking
                    Apartment apartment = apartmentRepository.findById(payment.getApartmentId()).orElse(null);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("hasBooking", true);
                    response.put("apartmentId", payment.getApartmentId());
                    response.put("apartmentTitle", apartment != null ? apartment.getTitle() : "Unknown");
                    response.put("monthlyRent", apartment != null ? apartment.getMonthlyRate() : 0);
                    response.put("transactionId", payment.getTransactionId());
                    response.put("paymentDate", payment.getCreatedAt());
                    
                    return ResponseEntity.ok(response);
                }
            }
            
            // No active booking found
            Map<String, Object> response = new HashMap<>();
            response.put("hasBooking", false);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check booking status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
