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
     * 
     * Uses apartment.booked status as the source of truth
     */
    @GetMapping("/{userId}/booking-status")
    public ResponseEntity<?> getBookingStatus(@PathVariable Long userId) {
        try {
            // Get all apartments
            List<Apartment> allApartments = apartmentRepository.findAll();
            
            // Find any apartment that is BOOKED by this tenant
            for (Apartment apartment : allApartments) {
                if (apartment.isBooked()) {
                    // Check if this tenant has a COMPLETED payment for this apartment
                    List<Payment> payments = paymentRepository.findByTenantId(userId);
                    
                    for (Payment payment : payments) {
                        if ("COMPLETED".equalsIgnoreCase(payment.getStatus()) && 
                            payment.getApartmentId() != null &&
                            payment.getApartmentId().equals(apartment.getApartmentId())) {
                            
                            // This tenant has the booked apartment
                            Map<String, Object> response = new HashMap<>();
                            response.put("hasBooking", true);
                            response.put("apartmentId", apartment.getApartmentId());
                            response.put("apartmentTitle", apartment.getTitle());
                            response.put("monthlyRent", apartment.getMonthlyRate());
                            response.put("transactionId", payment.getTransactionId());
                            response.put("paymentDate", payment.getCreatedAt());
                            
                            return ResponseEntity.ok(response);
                        }
                    }
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
