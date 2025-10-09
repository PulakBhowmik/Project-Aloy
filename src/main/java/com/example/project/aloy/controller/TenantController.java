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
            
            // Look for a COMPLETED payment (active booking) that hasn't been vacated
            for (Payment payment : payments) {
                if ("COMPLETED".equalsIgnoreCase(payment.getStatus()) && 
                    payment.getApartmentId() != null && 
                    payment.getVacateDate() == null) {  // Only non-vacated bookings
                    
                    // Found active booking - fetch apartment details
                    Apartment apartment = apartmentRepository.findById(payment.getApartmentId()).orElse(null);
                    
                    if (apartment == null) {
                        System.out.println("[WARN] Apartment with ID " + payment.getApartmentId() + " not found for payment " + payment.getPaymentId());
                        continue; // Skip this payment and check next one
                    }
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("hasBooking", true);
                    response.put("apartmentId", payment.getApartmentId());
                    response.put("apartmentTitle", apartment.getTitle());
                    response.put("monthlyRent", apartment.getMonthlyRate());
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
