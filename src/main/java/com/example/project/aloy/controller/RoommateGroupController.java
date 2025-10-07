package com.example.project.aloy.controller;

import com.example.project.aloy.model.RoommateGroup;
import com.example.project.aloy.model.User;
import com.example.project.aloy.service.RoommateGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class RoommateGroupController {

    @Autowired
    private RoommateGroupService groupService;

    /**
     * Create a new group for an apartment
     * POST /api/groups/create
     * Body: { "apartmentId": 1, "creatorId": 7 }
     */
    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Long> request) {
        try {
            Long apartmentId = request.get("apartmentId");
            Long creatorId = request.get("creatorId");
            
            RoommateGroup group = groupService.createGroup(apartmentId, creatorId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("group", group);
            response.put("inviteCode", group.getInviteCode());
            response.put("inviteLink", "/join-group?code=" + group.getInviteCode());
            response.put("message", "Group created successfully! Share the invite code: " + group.getInviteCode());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Join a group using invite code
     * POST /api/groups/join
     * Body: { "inviteCode": "ABC123", "tenantId": 8 }
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinGroup(@RequestBody Map<String, Object> request) {
        try {
            String inviteCode = (String) request.get("inviteCode");
            Long tenantId = Long.valueOf(request.get("tenantId").toString());
            
            RoommateGroup group = groupService.joinGroup(inviteCode, tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("group", group);
            response.put("memberCount", group.getMemberCount());
            response.put("isFull", group.isFull());
            response.put("message", "Successfully joined the group! (" + group.getMemberCount() + "/4 members)");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Leave a group
     * POST /api/groups/{groupId}/leave
     * Body: { "tenantId": 8 }
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable Long groupId, @RequestBody Map<String, Long> request) {
        try {
            Long tenantId = request.get("tenantId");
            groupService.leaveGroup(groupId, tenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully left the group");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Book apartment (payment)
     * POST /api/groups/{groupId}/book
     * Body: { "tenantId": 7 }
     */
    @PostMapping("/{groupId}/book")
    public ResponseEntity<?> bookApartment(@PathVariable Long groupId, @RequestBody Map<String, Long> request) {
        try {
            Long payingTenantId = request.get("tenantId");
            RoommateGroup group = groupService.bookApartment(groupId, payingTenantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("group", group);
            response.put("message", "Apartment booked successfully!");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get forming and ready groups for an apartment
     * GET /api/groups/apartment/{apartmentId}
     */
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<?> getGroupsForApartment(@PathVariable Long apartmentId) {
        try {
            List<RoommateGroup> formingGroups = groupService.getFormingGroups(apartmentId);
            List<RoommateGroup> readyGroups = groupService.getReadyGroups(apartmentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("formingGroups", formingGroups);
            response.put("readyGroups", readyGroups);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get group details by invite code
     * GET /api/groups/code/{inviteCode}
     */
    @GetMapping("/code/{inviteCode}")
    public ResponseEntity<?> getGroupByCode(@PathVariable String inviteCode) {
        try {
            Optional<RoommateGroup> group = groupService.getGroupByInviteCode(inviteCode);
            
            if (group.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Invalid invite code"));
            }
            
            RoommateGroup g = group.get();
            Map<String, Object> response = new HashMap<>();
            response.put("group", g);
            response.put("apartment", g.getApartment());
            response.put("memberCount", g.getMemberCount());
            response.put("members", g.getMembers().stream().map(m -> {
                User tenant = m.getTenant();
                Map<String, Object> memberInfo = new HashMap<>();
                memberInfo.put("userId", tenant.getUserId());
                memberInfo.put("name", tenant.getName());
                memberInfo.put("joinedAt", m.getJoinedAt());
                return memberInfo;
            }).toList());
            response.put("status", g.getStatus());
            response.put("isFull", g.isFull());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get group details by group ID
     * GET /api/groups/{groupId}
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupById(@PathVariable Long groupId) {
        try {
            Optional<RoommateGroup> group = groupService.getGroupById(groupId);
            
            if (group.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Group not found"));
            }
            
            return ResponseEntity.ok(group.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
