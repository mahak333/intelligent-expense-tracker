package com.expensetracker.service;

import com.expensetracker.dto.ExpenseRequest;
import com.expensetracker.model.*;
import com.expensetracker.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MLService mlService;

    @Autowired
    private AlertRepository alertRepository;

    /**
     * Add a new expense, auto-categorize using ML if category not provided
     */
    public Expense addExpense(Long userId, ExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Auto-categorize using ML
        String predictedCategory = mlService.predictCategory(request.getDescription());
        String finalCategory = (request.getCategory() != null && !request.getCategory().isEmpty())
                ? request.getCategory() : predictedCategory;

        Expense expense = Expense.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .category(finalCategory)
                .predictedCategory(predictedCategory)
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDateTime.now())
                .notes(request.getNotes())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .user(user)
                .build();

        Expense saved = expenseRepository.save(expense);

        // Check for overspending after adding
        checkOverspendingAlert(userId, finalCategory);

        return saved;
    }

    /**
     * Get all expenses for a user
     */
    public List<Expense> getUserExpenses(Long userId) {
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(userId);
    }

    /**
     * Get expenses within a date range
     */
    public List<Expense> getExpensesByDateRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return expenseRepository.findByUserIdAndExpenseDateBetween(userId, start, end);
    }

    /**
     * Delete an expense
     */
    public void deleteExpense(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        expenseRepository.delete(expense);
    }

    /**
     * Get category breakdown for dashboard
     */
    public Map<String, Double> getCategoryBreakdown(Long userId) {
        List<Object[]> results = expenseRepository.getCategoryTotals(userId);
        Map<String, Double> breakdown = new LinkedHashMap<>();
        for (Object[] row : results) {
            breakdown.put((String) row[0], ((Number) row[1]).doubleValue());
        }
        return breakdown;
    }

    /**
     * Get monthly totals for trend analysis
     */
    public Map<String, Double> getMonthlyTrends(Long userId, int year) {
        List<Object[]> results = expenseRepository.getMonthlyTotals(userId, year);
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        Map<String, Double> trends = new LinkedHashMap<>();
        for (String m : months) trends.put(m, 0.0);
        for (Object[] row : results) {
            int monthNum = ((Number) row[0]).intValue();
            trends.put(months[monthNum - 1], ((Number) row[1]).doubleValue());
        }
        return trends;
    }

    /**
     * Overspending alert check: compares current week vs last 4-week average
     */
    private void checkOverspendingAlert(Long userId, String category) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime weekStart = now.minusDays(7);
            LocalDateTime monthStart = now.minusDays(28);

            Double currentWeek = expenseRepository.getTotalAmountByDateRange(userId, weekStart, now);
            Double lastMonth = expenseRepository.getTotalAmountByDateRange(userId, monthStart, weekStart);

            if (currentWeek == null) currentWeek = 0.0;
            if (lastMonth == null) lastMonth = 0.0;

            double weeklyAvg = lastMonth / 4.0;

            if (weeklyAvg > 0 && currentWeek > weeklyAvg * 1.3) {
                double pct = Math.round((currentWeek / weeklyAvg - 1) * 100);
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    Alert alert = Alert.builder()
                            .message("⚠️ You are spending " + (int) pct + "% more than your weekly average!")
                            .type("WARNING")
                            .user(user)
                            .build();
                    alertRepository.save(alert);
                }
            }
        } catch (Exception e) {
            // Non-critical, ignore
        }
    }
}