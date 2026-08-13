package com.hackathon.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> health() {
        return Map.of(
                "status", "Privacy-Preserving Data Sharing Gateway is running",
                "ps_id", "PS26SCS211"
        );
    }
}
