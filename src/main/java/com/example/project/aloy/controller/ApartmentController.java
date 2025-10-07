package com.example.project.aloy.controller;

import com.example.project.aloy.model.Apartment;
import com.example.project.aloy.repository.ApartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/apartments")
@CrossOrigin(origins = "*")
public class ApartmentController {

    @Autowired
    private ApartmentRepository apartmentRepository;

    // Get all apartments (show all, frontend will display booking status)
    @GetMapping
    public List<Apartment> getAllApartments() {
        return apartmentRepository.findAll();
    }

    // Get apartment by ID
    @GetMapping("/{id}")
    public ResponseEntity<Apartment> getApartmentById(@PathVariable Long id) {
        Optional<Apartment> apartment = apartmentRepository.findById(id);
        return apartment.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get all apartments by owner ID
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Apartment>> getApartmentsByOwnerId(@PathVariable Long ownerId) {
        List<Apartment> apartments = apartmentRepository.findByOwnerId(ownerId);
        return ResponseEntity.ok(apartments);
    }

    // Create new apartment (for owners)
    @PostMapping
    public ResponseEntity<?> createApartment(@RequestBody Apartment apartment) {
        try {
            // Validate required fields
            if (apartment.getTitle() == null || apartment.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Title is required");
            }
            if (apartment.getMonthlyRate() == null) {
                return ResponseEntity.badRequest().body("Monthly rate is required");
            }
            if (apartment.getOwnerId() == null) {
                return ResponseEntity.badRequest().body("Owner ID is required");
            }

            Apartment savedApartment = apartmentRepository.save(apartment);
            return ResponseEntity.ok(savedApartment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Update apartment
    @PutMapping("/{id}")
    public ResponseEntity<?> updateApartment(@PathVariable Long id, @RequestBody Apartment apartmentDetails) {
        try {
            Optional<Apartment> existingApartment = apartmentRepository.findById(id);
            if (existingApartment.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Apartment apartment = existingApartment.get();
            
            // Verify that the owner is updating their own apartment (frontend will send ownerId)
            if (apartmentDetails.getOwnerId() != null && 
                !apartment.getOwnerId().equals(apartmentDetails.getOwnerId())) {
                return ResponseEntity.status(403).body("You can only edit your own apartments");
            }
            
            // Update fields
            if (apartmentDetails.getTitle() != null) {
                apartment.setTitle(apartmentDetails.getTitle());
            }
            if (apartmentDetails.getDescription() != null) {
                apartment.setDescription(apartmentDetails.getDescription());
            }
            if (apartmentDetails.getMonthlyRate() != null) {
                apartment.setMonthlyRate(apartmentDetails.getMonthlyRate());
            }
            if (apartmentDetails.getAvailability() != null) {
                apartment.setAvailability(apartmentDetails.getAvailability());
            }
            if (apartmentDetails.getDistrict() != null) {
                apartment.setDistrict(apartmentDetails.getDistrict());
            }
            if (apartmentDetails.getStreet() != null) {
                apartment.setStreet(apartmentDetails.getStreet());
            }
            if (apartmentDetails.getHouseNo() != null) {
                apartment.setHouseNo(apartmentDetails.getHouseNo());
            }
            if (apartmentDetails.getAddress() != null) {
                apartment.setAddress(apartmentDetails.getAddress());
            }
            if (apartmentDetails.getAllowedFor() != null) {
                apartment.setAllowedFor(apartmentDetails.getAllowedFor());
            }

            Apartment updatedApartment = apartmentRepository.save(apartment);
            return ResponseEntity.ok(updatedApartment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Delete apartment
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApartment(@PathVariable Long id) {
        try {
            if (!apartmentRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            apartmentRepository.deleteById(id);
            return ResponseEntity.ok("Apartment deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Search apartments by criteria
    @GetMapping("/search")
    public List<Apartment> searchApartments(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) Integer bedrooms) {

    List<Apartment> apartments = apartmentRepository.findAll();

        // Filter by district
        if (district != null && !district.trim().isEmpty()) {
            apartments = apartments.stream()
                    .filter(a -> a.getDistrict() != null &&
                            a.getDistrict().toLowerCase().contains(district.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Filter by price range
        if (minPrice != null) {
            apartments = apartments.stream()
                    .filter(a -> a.getMonthlyRate() != null && a.getMonthlyRate().compareTo(minPrice) >= 0)
                    .collect(Collectors.toList());
        }

        if (maxPrice != null) {
            apartments = apartments.stream()
                    .filter(a -> a.getMonthlyRate() != null && a.getMonthlyRate().compareTo(maxPrice) <= 0)
                    .collect(Collectors.toList());
        }

        // Filter by availability
        if (availability != null && !availability.trim().isEmpty()) {
            try {
                LocalDate availDate = LocalDate.parse(availability);
                apartments = apartments.stream()
                        .filter(a -> a.getAvailability() != null && !a.getAvailability().isBefore(availDate))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // If date parsing fails, ignore this filter
            }
        }

        return apartments;
    }
}