package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.model.Expense;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.ExpenseService;
import com.expensetracker.service.MLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private MLService mlService;

    @Autowired
    private UserRepository userRepository;

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }

    @PostMapping
    public ResponseEntity<?> addExpense(@RequestBody ExpenseRequest request, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            Expense expense = expenseService.addExpense(userId, request);
            return ResponseEntity.ok(expense);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getExpenses(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(expenseService.getUserExpenses(userId));
    }

    @GetMapping("/range")
    public ResponseEntity<?> getByRange(@RequestParam String start,
                                        @RequestParam String end,
                                        Authentication auth) {
        Long userId = getUserId(auth);
        LocalDateTime startDate = LocalDateTime.parse(start);
        LocalDateTime endDate = LocalDateTime.parse(end);
        return ResponseEntity.ok(expenseService.getExpensesByDateRange(userId, startDate, endDate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = getUserId(auth);
            expenseService.deleteExpense(id, userId);
            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analytics/categories")
    public ResponseEntity<?> getCategoryBreakdown(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(expenseService.getCategoryBreakdown(userId));
    }

    @GetMapping("/analytics/monthly")
    public ResponseEntity<?> getMonthlyTrends(@RequestParam(defaultValue = "2024") int year,
                                              Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(expenseService.getMonthlyTrends(userId, year));
    }

    @GetMapping("/predict/next-month")
    public ResponseEntity<?> predictNextMonth(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(mlService.getPrediction(userId));
    }

    @PostMapping("/ocr")
    public ResponseEntity<?> extractReceiptData(@RequestBody Map<String, String> body) {
        String imageBase64 = body.get("image");
        return ResponseEntity.ok(mlService.extractReceiptData(imageBase64));
    }
}