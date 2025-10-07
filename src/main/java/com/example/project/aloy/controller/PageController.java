package com.example.project.aloy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/apartment-details")
    public String apartmentDetails() {
        return "apartment-details";
    }

    @GetMapping("/join-group")
    public String joinGroup() {
        return "join-group";
    }

    @GetMapping("/owner-dashboard")
    public String ownerDashboard() {
        return "owner-dashboard";
    }
}