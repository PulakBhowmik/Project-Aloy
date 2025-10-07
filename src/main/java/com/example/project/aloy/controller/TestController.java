//package com.example.project.aloy.controller;
//
//import com.example.project.aloy.model.Apartment;
//import com.example.project.aloy.model.User;
//import com.example.project.aloy.repository.ApartmentRepository;
//import com.example.project.aloy.repository.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.List;
//
//@RestController
//public class TestController {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private ApartmentRepository apartmentRepository;
//
//    @GetMapping("/test")
//    public String test() {
//        return "✅ Everything is working! Database connected successfully!";
//    }
//
//    @PostMapping("/test-data")
//    public String createTestData() {
//        try {
//            // Create a test user with password
//            User user = new User();
//            user.setName("Test User");
//            user.setEmail("test@example.com");
//            user.setPhoneNumber("123-456-7890");
//            user.setPassword("test123"); // Add password
//            user.setRole("tenant");
//            user.setDistrict("Downtown");
//            User savedUser = userRepository.save(user);
//
//            // Create a test apartment
//            Apartment apartment = new Apartment();
//            apartment.setTitle("Luxury Downtown Apartment");
//            apartment.setDescription("Beautiful apartment with great view");
//            apartment.setMonthlyRate(new BigDecimal("1200.00"));
//            apartment.setAvailability(LocalDate.now().plusMonths(1));
//            apartment.setOwnerId(savedUser.getUserId());
//            apartment.setDistrict("Downtown");
//            apartment.setStreet("Main Street");
//            apartment.setHouseNo("123");
//            apartment.setAddress("123 Main Street, Downtown");
//            apartmentRepository.save(apartment);
//
//            return "✅ Test data created successfully! User ID: " + savedUser.getUserId();
//        } catch (Exception e) {
//            return "❌ Error creating test  " + e.getMessage();
//        }
//    }
//
//    @GetMapping("/users")
//    public List<User> getAllUsers() {
//        return userRepository.findAll();
//    }
//
//    @GetMapping("/apartments")
//    public List<Apartment> getAllApartments() {
//        return apartmentRepository.findAll();
//    }
//}

package com.example.project.aloy.controller;

import com.example.project.aloy.model.Apartment;
import com.example.project.aloy.model.User;
import com.example.project.aloy.repository.ApartmentRepository;
import com.example.project.aloy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @GetMapping("/test")
    public String test() {
        return "✅ Everything is working! Database connected successfully!";
    }

    // Option 1: Keep as POST but add a GET endpoint for browser testing
    @PostMapping("/test-data")
    public String createTestData() {
        return createTestUserData();
    }

    // Option 2: Add a GET endpoint for easy browser testing
    @GetMapping("/test-data")
    public String createTestDataViaGet() {
        return createTestUserData();
    }

    // Shared method for both POST and GET
    private String createTestUserData() {
        try {
            // Create a test user with password
            User user = new User();
            user.setName("Test User");
            user.setEmail("test@example.com");
            user.setPhoneNumber("123-456-7890");
            user.setPassword("test123"); // Set password
            user.setRole("TENANT");
            user.setDistrict("Downtown");
            User savedUser = userRepository.save(user);

            // Create a test apartment
            Apartment apartment = new Apartment();
            apartment.setTitle("Luxury Downtown Apartment");
            apartment.setDescription("Beautiful apartment with great view");
            apartment.setMonthlyRate(new BigDecimal("1200.00"));
            apartment.setAvailability(LocalDate.now().plusMonths(1));
            apartment.setOwnerId(savedUser.getUserId());
            apartment.setDistrict("Downtown");
            apartment.setStreet("Main Street");
            apartment.setHouseNo("123");
            apartment.setAddress("123 Main Street, Downtown");
            apartmentRepository.save(apartment);

            return "✅ Test data created successfully! User ID: " + savedUser.getUserId();
        } catch (Exception e) {
            return "❌ Error creating test  " + e.getMessage() + " - " + e.getClass().getSimpleName();
        }
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/apartments")
    public List<Apartment> getAllApartments() {
        return apartmentRepository.findAll();
    }
}