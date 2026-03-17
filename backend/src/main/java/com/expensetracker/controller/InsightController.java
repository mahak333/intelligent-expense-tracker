package com.expensetracker.controller;

import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.InsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*")
public class InsightController {

    @Autowired
    private InsightService insightService;

    @Autowired
    private UserRepository userRepository;

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }

    @GetMapping
    public ResponseEntity<?> getInsights(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(insightService.generateInsights(userId));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> getSuggestions(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(insightService.getSuggestions(userId));
    }
}