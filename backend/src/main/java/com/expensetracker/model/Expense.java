package com.expensetracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String category;

    @Column(name = "predicted_category")
    private String predictedCategory;

    @Column(name = "expense_date")
    private LocalDateTime expenseDate = LocalDateTime.now();

    @Column(name = "receipt_image_url")
    private String receiptImageUrl;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}