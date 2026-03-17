package com.expensetracker.repository;

import com.expensetracker.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);

    List<Expense> findByUserIdAndExpenseDateBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId GROUP BY e.category")
    List<Object[]> getCategoryTotals(@Param("userId") Long userId);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId " +
            "AND e.expenseDate BETWEEN :start AND :end GROUP BY e.category")
    List<Object[]> getCategoryTotalsByDateRange(@Param("userId") Long userId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("SELECT MONTH(e.expenseDate), SUM(e.amount) FROM Expense e " +
            "WHERE e.user.id = :userId AND YEAR(e.expenseDate) = :year GROUP BY MONTH(e.expenseDate)")
    List<Object[]> getMonthlyTotals(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId " +
            "AND e.expenseDate BETWEEN :start AND :end")
    Double getTotalAmountByDateRange(@Param("userId") Long userId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
}