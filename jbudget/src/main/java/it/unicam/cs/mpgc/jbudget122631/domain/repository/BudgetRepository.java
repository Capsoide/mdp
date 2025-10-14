package it.unicam.cs.mpgc.jbudget122631.domain.repository;

import it.unicam.cs.mpgc.jbudget122631.domain.model.Budget;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Period;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository {

    Budget save(Budget budget);
    Optional<Budget> findById(Long id);
    List<Budget> findAll();
    void delete(Budget budget);
    void deleteById(Long id);

    Optional<Budget> findByPeriodAndCategory(Period period, Category category);
    List<Budget> findByPeriod(Period period);
    List<Budget> findByCategory(Category category);
    List<Budget> findGeneralBudgets();
    List<Budget> findActiveBudgets();

    List<Budget> findOverBudgets();
    List<Budget> findByPeriodOrderByVariance(Period period);

    long count();
}