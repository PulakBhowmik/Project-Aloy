package com.example.project.aloy.controller;

import com.example.project.aloy.model.ApartmentImage;
import com.example.project.aloy.model.Apartment;
import com.example.project.aloy.repository.ApartmentImageRepository;
import com.example.project.aloy.repository.ApartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/apartment-images")
@CrossOrigin(origins = "*")
public class ApartmentImageController {

    @Autowired
    private ApartmentImageRepository apartmentImageRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    // Upload image (owner)
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam Long apartmentId, @RequestParam String imageUrl) {
        Optional<Apartment> apartmentOpt = apartmentRepository.findById(apartmentId);
        if (apartmentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Apartment not found");
        }
        ApartmentImage image = new ApartmentImage();
        image.setApartment(apartmentOpt.get());
        image.setImageUrl(imageUrl);
        ApartmentImage saved = apartmentImageRepository.save(image);
        return ResponseEntity.ok(saved);
    }

    // Get images for apartment
    @GetMapping("/by-apartment/{apartmentId}")
    public List<ApartmentImage> getImages(@PathVariable Long apartmentId) {
        return apartmentImageRepository.findAll()
                .stream()
                .filter(img -> img.getApartment().getApartmentId().equals(apartmentId))
                .toList();
    }
}
