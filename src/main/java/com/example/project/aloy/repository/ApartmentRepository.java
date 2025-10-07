package com.example.project.aloy.repository;

import com.example.project.aloy.model.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
	// Find all apartments that are not booked
	java.util.List<com.example.project.aloy.model.Apartment> findByBookedFalse();
	
	// Find all available apartments (not booked AND status is AVAILABLE)
	@Query("SELECT a FROM Apartment a WHERE a.booked = false AND (a.status = 'AVAILABLE' OR a.status IS NULL)")
	java.util.List<Apartment> findAvailableApartments();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Apartment a where a.apartmentId = :id")
	java.util.Optional<Apartment> findByIdForUpdate(@Param("id") Long id);
}