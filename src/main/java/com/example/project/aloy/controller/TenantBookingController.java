package com.example.project.aloy.controller;

import com.example.project.aloy.model.Payment;
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
public class TenantBookingController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    /**
     * Check if a tenant already has an active booking.
     * Returns booking status and apartment details if exists.
     */
    @GetMapping("/{tenantId}/booking-status")
    public ResponseEntity<Map<String, Object>> getTenantBookingStatus(@PathVariable Long tenantId) {
        Map<String, Object> response = new HashMap<>();
        
        // Find all payments for this tenant
        List<Payment> tenantPayments = paymentRepository.findByTenantId(tenantId);
        
        for (Payment payment : tenantPayments) {
            if ("COMPLETED".equalsIgnoreCase(payment.getStatus()) && payment.getApartmentId() != null) {
                // Tenant has an active booking
                response.put("hasBooking", true);
                response.put("apartmentId", payment.getApartmentId());
                response.put("paymentId", payment.getPaymentId());
                response.put("transactionId", payment.getTransactionId());
                response.put("amount", payment.getAmount());
                
                // Fetch apartment details
                apartmentRepository.findById(payment.getApartmentId()).ifPresent(apartment -> {
                    response.put("apartmentTitle", apartment.getTitle());
                    response.put("apartmentAddress", apartment.getAddress());
                    response.put("monthlyRent", apartment.getMonthlyRate());
                });
                
                return ResponseEntity.ok(response);
            }
        }
        
        // No active booking found
        response.put("hasBooking", false);
        return ResponseEntity.ok(response);
    }
}
