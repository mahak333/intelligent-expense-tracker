package com.expensetracker.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExpenseRequest {
    private String description;
    private Double amount;
    private String category; // optional, ML will predict if empty
    private LocalDateTime expenseDate;
    private String notes;
    private Boolean isRecurring;
}
