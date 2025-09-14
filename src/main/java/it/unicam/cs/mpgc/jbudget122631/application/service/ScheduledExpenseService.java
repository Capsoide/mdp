package it.unicam.cs.mpgc.jbudget122631.application.service;

import it.unicam.cs.mpgc.jbudget122631.domain.model.ScheduledExpense;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Movement;

import java.util.List;
import java.util.Optional;

public interface ScheduledExpenseService {

    // CRUD operations
    ScheduledExpense createScheduledExpense(ScheduledExpense scheduledExpense);
    Optional<ScheduledExpense> getScheduledExpenseById(Long id);
    List<ScheduledExpense> getAllScheduledExpenses();
    ScheduledExpense updateScheduledExpense(Long id, ScheduledExpense scheduledExpense);
    void deleteScheduledExpense(Long id);

    // Due date operations
    List<ScheduledExpense> getDueExpenses();
    List<ScheduledExpense> getOverdueExpenses();
    List<ScheduledExpense> getExpensesDueInDays(int days);

    // Completion operations
    Movement completeScheduledExpense(Long id);
    void markAsCompleted(Long id);

    // Recurrence operations
    List<ScheduledExpense> getRecurringExpenses();
    ScheduledExpense createNextOccurrence(Long id);
    void processRecurringExpenses(); // Background task

    // Notifications
    List<ScheduledExpense> getExpensesRequiringAttention();
}