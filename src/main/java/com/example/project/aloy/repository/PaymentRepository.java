package com.example.project.aloy.repository;

import com.example.project.aloy.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	java.util.Optional<com.example.project.aloy.model.Payment> findByTransactionId(String transactionId);
	java.util.Optional<com.example.project.aloy.model.Payment> findByApartmentId(Long apartmentId);
	java.util.List<com.example.project.aloy.model.Payment> findAllByApartmentId(Long apartmentId);
	java.util.List<com.example.project.aloy.model.Payment> findByTenantId(Long tenantId);
	java.util.Optional<com.example.project.aloy.model.Payment> findByTenantIdAndApartmentId(Long tenantId, Long apartmentId);
}