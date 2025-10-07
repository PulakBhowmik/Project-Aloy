package com.example.project.aloy.controller;

import com.example.project.aloy.repository.ApartmentRepository;
import com.example.project.aloy.repository.PaymentRepository;
import com.example.project.aloy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/owner-analytics")
@CrossOrigin(origins = "*")
public class OwnerAnalyticsController {

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get detailed analytics for an owner
     * Demonstrates: Complex JOIN queries, Aggregate functions (COUNT, SUM, AVG)
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<Map<String, Object>> getOwnerAnalytics(@PathVariable Long ownerId) {
        Map<String, Object> analytics = new HashMap<>();

        try {
            // Query 1: Get all apartments with their booking status
            // SQL: SELECT * FROM apartments WHERE owner_id = ?
            var apartments = apartmentRepository.findByOwnerId(ownerId);
            analytics.put("totalApartments", apartments.size());

            // Query 2: Count available apartments
            // SQL: SELECT COUNT(*) FROM apartments WHERE owner_id = ? AND booked = false
            long availableCount = apartments.stream()
                    .filter(apt -> !apt.isBooked())
                    .count();
            analytics.put("availableApartments", availableCount);

            // Query 3: Count rented apartments
            // SQL: SELECT COUNT(*) FROM apartments WHERE owner_id = ? AND booked = true
            long rentedCount = apartments.stream()
                    .filter(apt -> apt.isBooked())
                    .count();
            analytics.put("rentedApartments", rentedCount);

            // Query 4: Calculate total monthly revenue from rented apartments
            // SQL: SELECT SUM(monthly_rent) FROM apartments WHERE owner_id = ? AND booked = true
            double totalRevenue = apartments.stream()
                    .filter(apt -> apt.isBooked())
                    .mapToDouble(apt -> apt.getMonthlyRate() != null ? apt.getMonthlyRate().doubleValue() : 0)
                    .sum();
            analytics.put("monthlyRevenue", totalRevenue);

            // Query 5: Calculate average rent
            // SQL: SELECT AVG(monthly_rent) FROM apartments WHERE owner_id = ?
            double avgRent = apartments.stream()
                    .filter(apt -> apt.getMonthlyRate() != null)
                    .mapToDouble(apt -> apt.getMonthlyRate().doubleValue())
                    .average()
                    .orElse(0.0);
            analytics.put("averageRent", avgRent);

            // Query 6: Occupancy rate percentage
            double occupancyRate = apartments.isEmpty() ? 0 : (rentedCount * 100.0 / apartments.size());
            analytics.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);

            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get tenant details for rented apartments
     * Demonstrates: INNER JOIN between apartments, payment, and users tables
     * SQL: SELECT a.*, u.name, u.email, p.transaction_id, p.amount, p.created_at
     *      FROM apartments a
     *      INNER JOIN payment p ON a.apartment_id = p.apartment_id
     *      INNER JOIN users u ON p.tenant_id = u.user_id
     *      WHERE a.owner_id = ? AND a.booked = true
     */
    @GetMapping("/owner/{ownerId}/tenants")
    public ResponseEntity<List<Map<String, Object>>> getTenantsInfo(@PathVariable Long ownerId) {
        List<Map<String, Object>> tenantsList = new ArrayList<>();

        try {
            // Get all apartments owned by this owner
            var apartments = apartmentRepository.findByOwnerId(ownerId);

            for (var apartment : apartments) {
                if (apartment.isBooked()) {
                    // Find payment record for this apartment
                    var payments = paymentRepository.findAllByApartmentId(apartment.getApartmentId());
                    
                    for (var payment : payments) {
                        // Only include COMPLETED payments
                        if ("COMPLETED".equals(payment.getStatus())) {
                            Map<String, Object> tenantInfo = new HashMap<>();
                            tenantInfo.put("apartmentId", apartment.getApartmentId());
                            tenantInfo.put("apartmentTitle", apartment.getTitle());
                            tenantInfo.put("monthlyRent", apartment.getMonthlyRate());
                            tenantInfo.put("district", apartment.getDistrict());
                            
                            // Get tenant information
                            if (payment.getTenantId() != null) {
                                var tenantOpt = userRepository.findById(payment.getTenantId());
                                if (tenantOpt.isPresent()) {
                                    var tenant = tenantOpt.get();
                                    tenantInfo.put("tenantId", tenant.getUserId());
                                    tenantInfo.put("tenantName", tenant.getName());
                                    tenantInfo.put("tenantEmail", tenant.getEmail());
                                }
                            }
                            
                            tenantInfo.put("transactionId", payment.getTransactionId());
                            tenantInfo.put("paymentAmount", payment.getAmount());
                            tenantInfo.put("paymentDate", payment.getCreatedAt());
                            tenantInfo.put("vacateDate", payment.getVacateDate());
                            
                            tenantsList.add(tenantInfo);
                        }
                    }
                }
            }

            return ResponseEntity.ok(tenantsList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Get revenue by district
     * Demonstrates: GROUP BY with aggregate functions
     * SQL: SELECT district, COUNT(*) as count, SUM(monthly_rent) as revenue
     *      FROM apartments WHERE owner_id = ? AND booked = true
     *      GROUP BY district
     *      ORDER BY revenue DESC
     */
    @GetMapping("/owner/{ownerId}/revenue-by-district")
    public ResponseEntity<List<Map<String, Object>>> getRevenueByDistrict(@PathVariable Long ownerId) {
        try {
            var apartments = apartmentRepository.findByOwnerId(ownerId);
            
            Map<String, Map<String, Object>> districtMap = new HashMap<>();
            
            for (var apt : apartments) {
                if (apt.isBooked() && apt.getDistrict() != null) {
                    String district = apt.getDistrict();
                    
                    districtMap.putIfAbsent(district, new HashMap<>());
                    Map<String, Object> districtData = districtMap.get(district);
                    
                    int count = (int) districtData.getOrDefault("count", 0);
                    double revenue = (double) districtData.getOrDefault("revenue", 0.0);
                    
                    districtData.put("district", district);
                    districtData.put("count", count + 1);
                    districtData.put("revenue", revenue + (apt.getMonthlyRate() != null ? apt.getMonthlyRate().doubleValue() : 0));
                }
            }
            
            List<Map<String, Object>> result = new ArrayList<>(districtMap.values());
            
            // Sort by revenue descending
            result.sort((a, b) -> Double.compare((double) b.get("revenue"), (double) a.get("revenue")));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Get payment history for owner's apartments
     * Demonstrates: Date range queries, multiple JOINs, ORDER BY
     * SQL: SELECT p.*, a.title, u.name
     *      FROM payment p
     *      INNER JOIN apartments a ON p.apartment_id = a.apartment_id
     *      INNER JOIN users u ON p.tenant_id = u.user_id
     *      WHERE a.owner_id = ?
     *      ORDER BY p.created_at DESC
     *      LIMIT ?
     */
    @GetMapping("/owner/{ownerId}/payment-history")
    public ResponseEntity<List<Map<String, Object>>> getPaymentHistory(
            @PathVariable Long ownerId,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        List<Map<String, Object>> paymentHistory = new ArrayList<>();

        try {
            var apartments = apartmentRepository.findByOwnerId(ownerId);
            
            for (var apartment : apartments) {
                var payments = paymentRepository.findAllByApartmentId(apartment.getApartmentId());
                
                for (var payment : payments) {
                    Map<String, Object> record = new HashMap<>();
                    record.put("paymentId", payment.getPaymentId());
                    record.put("transactionId", payment.getTransactionId());
                    record.put("apartmentTitle", apartment.getTitle());
                    record.put("amount", payment.getAmount());
                    record.put("status", payment.getStatus());
                    record.put("paymentDate", payment.getCreatedAt());
                    
                    // Get tenant name
                    if (payment.getTenantId() != null) {
                        var tenantOpt = userRepository.findById(payment.getTenantId());
                        if (tenantOpt.isPresent()) {
                            record.put("tenantName", tenantOpt.get().getName());
                        }
                    }
                    
                    paymentHistory.add(record);
                }
            }
            
            // Sort by payment date descending
            paymentHistory.sort((a, b) -> {
                if (a.get("paymentDate") == null) return 1;
                if (b.get("paymentDate") == null) return -1;
                return b.get("paymentDate").toString().compareTo(a.get("paymentDate").toString());
            });
            
            // Limit results
            if (paymentHistory.size() > limit) {
                paymentHistory = paymentHistory.subList(0, limit);
            }
            
            return ResponseEntity.ok(paymentHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Get top performing apartments by revenue
     * Demonstrates: ORDER BY with calculated fields, LIMIT
     * SQL: SELECT a.*, COUNT(p.payment_id) as booking_count, SUM(p.amount) as total_revenue
     *      FROM apartments a
     *      LEFT JOIN payment p ON a.apartment_id = p.apartment_id AND p.status = 'COMPLETED'
     *      WHERE a.owner_id = ?
     *      GROUP BY a.apartment_id
     *      ORDER BY total_revenue DESC
     *      LIMIT ?
     */
    @GetMapping("/owner/{ownerId}/top-apartments")
    public ResponseEntity<List<Map<String, Object>>> getTopApartments(
            @PathVariable Long ownerId,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        
        List<Map<String, Object>> topApartments = new ArrayList<>();

        try {
            var apartments = apartmentRepository.findByOwnerId(ownerId);
            
            for (var apartment : apartments) {
                Map<String, Object> aptData = new HashMap<>();
                aptData.put("apartmentId", apartment.getApartmentId());
                aptData.put("title", apartment.getTitle());
                aptData.put("district", apartment.getDistrict());
                aptData.put("monthlyRent", apartment.getMonthlyRate());
                aptData.put("status", apartment.getStatus());
                
                // Count bookings and calculate total revenue
                var payments = paymentRepository.findAllByApartmentId(apartment.getApartmentId());
                long bookingCount = payments.stream()
                        .filter(p -> "COMPLETED".equals(p.getStatus()))
                        .count();
                
                double totalRevenue = payments.stream()
                        .filter(p -> "COMPLETED".equals(p.getStatus()))
                        .mapToDouble(p -> p.getAmount() != null ? p.getAmount().doubleValue() : 0)
                        .sum();
                
                aptData.put("bookingCount", bookingCount);
                aptData.put("totalRevenue", totalRevenue);
                
                topApartments.add(aptData);
            }
            
            // Sort by total revenue descending
            topApartments.sort((a, b) -> Double.compare(
                    (double) b.get("totalRevenue"), 
                    (double) a.get("totalRevenue")));
            
            // Limit results
            if (topApartments.size() > limit) {
                topApartments = topApartments.subList(0, limit);
            }
            
            return ResponseEntity.ok(topApartments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
