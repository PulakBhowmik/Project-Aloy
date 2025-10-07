package com.example.project.aloy.controller;

import com.example.project.aloy.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vacate")
@CrossOrigin(origins = "*")
public class VacateController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> vacateApartment(@RequestBody Map<String, Object> request) {
        try {
            Long tenantId = Long.parseLong(request.get("tenantId").toString());
            Long apartmentId = Long.parseLong(request.get("apartmentId").toString());
            String vacateDate = request.get("vacateDate").toString();

            paymentService.vacateApartment(tenantId, apartmentId, vacateDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Apartment vacated successfully. The apartment is now available for booking.");
            response.put("vacateDate", vacateDate);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to vacate apartment: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
