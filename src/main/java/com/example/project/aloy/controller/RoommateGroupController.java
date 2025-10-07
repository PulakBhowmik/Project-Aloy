package com.example.project.aloy.controller;

import com.example.project.aloy.model.RoommateGroup;
import com.example.project.aloy.model.RoommateGroupMember;
import com.example.project.aloy.model.Apartment;
import com.example.project.aloy.model.User;
import com.example.project.aloy.repository.RoommateGroupRepository;
import com.example.project.aloy.repository.RoommateGroupMemberRepository;
import com.example.project.aloy.repository.ApartmentRepository;
import com.example.project.aloy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/roommate-groups")
@CrossOrigin(origins = "*")
public class RoommateGroupController {

    @Autowired
    private RoommateGroupRepository groupRepository;

    @Autowired
    private RoommateGroupMemberRepository memberRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private UserRepository userRepository;

    // Create roommate group (exactly 4 members)
    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestParam Long apartmentId, @RequestBody List<Long> tenantIds) {
        if (tenantIds.size() != 4) {
            return ResponseEntity.badRequest().body("Group must have exactly 4 members");
        }
        Optional<Apartment> apartmentOpt = apartmentRepository.findById(apartmentId);
        if (apartmentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Apartment not found");
        }
        RoommateGroup group = new RoommateGroup();
        group.setApartment(apartmentOpt.get());
        group = groupRepository.save(group);
        for (Long tenantId : tenantIds) {
            Optional<User> tenantOpt = userRepository.findById(tenantId);
            if (tenantOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Tenant not found: " + tenantId);
            }
            RoommateGroupMember member = new RoommateGroupMember();
            member.setGroup(group);
            member.setTenant(tenantOpt.get());
            memberRepository.save(member);
        }
        return ResponseEntity.ok(group);
    }

    // Get roommate groups for apartment
    @GetMapping("/by-apartment/{apartmentId}")
    public List<RoommateGroup> getGroups(@PathVariable Long apartmentId) {
        return groupRepository.findAll()
                .stream()
                .filter(g -> g.getApartment().getApartmentId().equals(apartmentId))
                .toList();
    }
}
