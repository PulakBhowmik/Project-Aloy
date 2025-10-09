package com.example.project.aloy.repository;

import com.example.project.aloy.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	java.util.Optional<com.example.project.aloy.model.Payment> findByTransactionId(String transactionId);
	java.util.Optional<com.example.project.aloy.model.Payment> findByApartmentId(Long apartmentId);
	java.util.List<com.example.project.aloy.model.Payment> findAllByApartmentId(Long apartmentId);
	java.util.List<com.example.project.aloy.model.Payment> findByTenantId(Long tenantId);
	
	// Find the most recent COMPLETED payment for a tenant and apartment
	@Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.apartmentId = :apartmentId AND p.status = 'COMPLETED' ORDER BY p.paymentId DESC")
	java.util.List<com.example.project.aloy.model.Payment> findCompletedPaymentsByTenantAndApartment(@Param("tenantId") Long tenantId, @Param("apartmentId") Long apartmentId);
}