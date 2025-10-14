package it.unicam.cs.mpgc.jbudget122631.domain.repository;

import it.unicam.cs.mpgc.jbudget122631.domain.model.ScheduledExpense;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.model.RecurrenceType;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Movement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduledExpenseRepository {

    ScheduledExpense save(ScheduledExpense scheduledExpense);
    Optional<ScheduledExpense> findById(Long id);
    List<ScheduledExpense> findAll();
    void delete(ScheduledExpense scheduledExpense);
    void deleteById(Long id);

    List<ScheduledExpense> findByDueDate(LocalDate dueDate);
    List<ScheduledExpense> findByDueDateBetween(LocalDate startDate, LocalDate endDate);
    List<ScheduledExpense> findOverdueExpenses();
    List<ScheduledExpense> findDueToday();
    List<ScheduledExpense> findDueThisWeek();

    List<ScheduledExpense> findByCompleted(boolean completed);
    List<ScheduledExpense> findActiveExpenses();

    List<ScheduledExpense> findByRecurrenceType(RecurrenceType recurrenceType);
    List<ScheduledExpense> findRecurringExpenses();

    List<ScheduledExpense> findByCategory(Category category);

    Optional<ScheduledExpense> findByCreatedMovement(Movement movement);

    long count();
}