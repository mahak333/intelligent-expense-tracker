package com.expensetracker.service;

import com.expensetracker.model.Expense;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InsightService {

    @Autowired
    private ExpenseRepository expenseRepository;

    /**
     * Generate behavioral insights from expense history
     */
    public List<String> generateInsights(Long userId) {
        List<String> insights = new ArrayList<>();
        List<Expense> expenses = expenseRepository.findByUserIdOrderByExpenseDateDesc(userId);

        if (expenses.isEmpty()) {
            insights.add("Start adding expenses to get personalized insights!");
            return insights;
        }

        // Weekend vs weekday spending
        double weekendTotal = expenses.stream()
                .filter(e -> {
                    int day = e.getExpenseDate().getDayOfWeek().getValue();
                    return day == 6 || day == 7;
                })
                .mapToDouble(Expense::getAmount).sum();
        double weekdayTotal = expenses.stream()
                .filter(e -> {
                    int day = e.getExpenseDate().getDayOfWeek().getValue();
                    return day < 6;
                })
                .mapToDouble(Expense::getAmount).sum();

        double weekendPerDay = weekendTotal / 2.0;
        double weekdayPerDay = weekdayTotal / 5.0;
        if (weekendPerDay > weekdayPerDay * 1.2) {
            insights.add("📅 You spend more on weekends — " +
                    String.format("%.0f%%", (weekendPerDay / weekdayPerDay - 1) * 100) + " more per day");
        }

        // Top spending category
        Map<String, Double> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)));
        String topCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        if (topCategory != null) {
            insights.add("🏆 Your highest spending category is " + topCategory);
        }

        // Frequent small purchases
        long smallPurchases = expenses.stream().filter(e -> e.getAmount() < 100).count();
        if (smallPurchases > 10) {
            insights.add("☕ You make many small purchases (" + smallPurchases +
                    " under ₹100) — they add up!");
        }

        // Smart suggestions
        if (topCategory != null) {
            double topAmount = categoryTotals.get(topCategory);
            double saving = topAmount * 0.15;
            insights.add("💡 Reducing " + topCategory + " spending by 15% could save you ₹" +
                    String.format("%.0f", saving) + " this month");
        }

        return insights;
    }

    /**
     * Smart financial suggestions
     */
    public List<Map<String, String>> getSuggestions(Long userId) {
        List<Map<String, String>> suggestions = new ArrayList<>();
        List<Expense> expenses = expenseRepository.findByUserIdOrderByExpenseDateDesc(userId);

        if (expenses.size() < 3) {
            suggestions.add(Map.of("type", "info", "message", "Add more expenses to get personalized suggestions"));
            return suggestions;
        }

        Map<String, Double> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)));

        double total = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            double pct = (entry.getValue() / total) * 100;
            if (entry.getKey().equals("Food") && pct > 35) {
                suggestions.add(Map.of("type", "warning",
                        "message", "🍕 Food spending is " + String.format("%.0f%%", pct) +
                                " of total. Consider meal prepping to save money."));
            }
            if (entry.getKey().equals("Entertainment") && pct > 20) {
                suggestions.add(Map.of("type", "warning",
                        "message", "🎮 Entertainment is " + String.format("%.0f%%", pct) +
                                " of budget. Consider free alternatives."));
            }
        }

        suggestions.add(Map.of("type", "tip",
                "message", "💰 Setting up a monthly budget helps reduce overspending by 23% on average"));

        return suggestions;
    }
}