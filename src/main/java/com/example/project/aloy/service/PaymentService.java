package com.example.project.aloy.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;
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
                    apt.setStatus("RENTED");
                    apartmentRepository.save(apt);
                    System.out.println("[SUCCESS Service] Apartment " + aptId + " successfully marked as BOOKED and RENTED");
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
}
