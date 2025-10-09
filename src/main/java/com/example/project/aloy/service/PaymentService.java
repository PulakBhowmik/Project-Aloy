package com.example.project.aloy.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;
import java.time.LocalDate;
import com.example.project.aloy.repository.PaymentRepository;
import com.example.project.aloy.repository.ApartmentRepository;
import com.example.project.aloy.model.Payment;
import com.example.project.aloy.model.Apartment;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    // Public transactional method - executed through Spring proxy
    @Transactional
    public void completePaymentAndMarkApartmentBooked(String tranId) {
        System.out.println("[DEBUG Service] Attempting to complete payment for tranId: " + tranId);
        Payment paymentRecord = paymentRepository.findByTransactionId(tranId).orElse(null);
        if (paymentRecord == null) {
            System.out.println("[WARNING Service] Payment record NOT found for transactionId: " + tranId);
            return;
        }

        System.out.println("[DEBUG Service] Payment record found: ID=" + paymentRecord.getPaymentId() + ", apartmentId=" + paymentRecord.getApartmentId() + ", status=" + paymentRecord.getStatus());

        // Update payment status to COMPLETED if not already
        if (!"COMPLETED".equals(paymentRecord.getStatus())) {
            paymentRecord.setStatus("COMPLETED");
            paymentRepository.save(paymentRecord);
            System.out.println("[DEBUG Service] Payment status updated to COMPLETED");
        }

        // Mark apartment as booked
        if (paymentRecord.getApartmentId() != null) {
            Long aptId = paymentRecord.getApartmentId();
            System.out.println("[DEBUG Service] Marking apartment " + aptId + " as booked");

            Optional<Apartment> lockedAptOpt = apartmentRepository.findByIdForUpdate(aptId);
            if (lockedAptOpt.isPresent()) {
                Apartment apt = lockedAptOpt.get();
                System.out.println("[DEBUG Service] Apartment found - current booked status: " + apt.isBooked() + ", status: " + apt.getStatus());

                if (!apt.isBooked()) {
                    apt.setBooked(true);
                    apt.setStatus("BOOKED");
                    apartmentRepository.save(apt);
                    System.out.println("[SUCCESS Service] Apartment " + aptId + " successfully marked as BOOKED");
                } else {
                    System.out.println("[INFO Service] Apartment " + aptId + " is already booked");
                }
            } else {
                System.out.println("[WARNING Service] Apartment " + aptId + " not found in database");
            }
        } else {
            System.out.println("[INFO Service] No apartment associated with this payment");
        }
    }

    /**
     * Allows a tenant to vacate an apartment by setting a vacate date
     * Automatically marks the apartment as AVAILABLE and updates availability date
     */
    @Transactional
    public void vacateApartment(Long tenantId, Long apartmentId, String vacateDate) {
        System.out.println("[DEBUG Service] Tenant " + tenantId + " vacating apartment " + apartmentId + " on " + vacateDate);
        
        // Find the most recent COMPLETED payment record for this tenant and apartment
        java.util.List<Payment> payments = paymentRepository.findCompletedPaymentsByTenantAndApartment(tenantId, apartmentId);
        
        if (payments.isEmpty()) {
            throw new RuntimeException("No completed booking found for this tenant and apartment");
        }
        
        // Get the most recent payment (first in list due to ORDER BY DESC)
        Payment payment = payments.get(0);
        System.out.println("[DEBUG Service] Found payment ID: " + payment.getPaymentId() + " with status: " + payment.getStatus());
        
        // Set the vacate date
        payment.setVacateDate(vacateDate);
        payment.setStatus("VACATED");
        paymentRepository.save(payment);
        System.out.println("[DEBUG Service] Payment record updated with vacate date");
        
        // Mark apartment as available and update availability date
        Optional<Apartment> apartmentOpt = apartmentRepository.findByIdForUpdate(apartmentId);
        if (apartmentOpt.isPresent()) {
            Apartment apartment = apartmentOpt.get();
            apartment.setBooked(false);
            apartment.setStatus("AVAILABLE");
            
            // Update the availability date to the vacate date
            try {
                LocalDate availabilityDate = LocalDate.parse(vacateDate);
                apartment.setAvailability(availabilityDate);
                System.out.println("[DEBUG Service] Apartment availability date updated to: " + vacateDate);
            } catch (Exception e) {
                System.out.println("[WARNING Service] Failed to parse vacate date, using current date");
                apartment.setAvailability(LocalDate.now());
            }
            
            apartmentRepository.save(apartment);
            System.out.println("[SUCCESS Service] Apartment " + apartmentId + " marked as AVAILABLE from " + vacateDate);
        }
    }
}
