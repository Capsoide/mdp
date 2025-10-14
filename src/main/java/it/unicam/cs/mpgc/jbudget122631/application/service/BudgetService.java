package it.unicam.cs.mpgc.jbudget122631.application.service;

import it.unicam.cs.mpgc.jbudget122631.application.dto.BudgetDTO;

import java.util.List;
import java.util.Optional;

public interface BudgetService {

    BudgetDTO createBudget(BudgetDTO budgetDTO);
    Optional<BudgetDTO> getBudgetById(Long id);
    List<BudgetDTO> getAllBudgets();
    BudgetDTO updateBudget(Long id, BudgetDTO budgetDTO);
    void deleteBudget(Long id);

    Optional<BudgetDTO> getBudgetByPeriodAndCategory(Long periodId, Long categoryId);
    List<BudgetDTO> getBudgetsByPeriod(Long periodId);
    List<BudgetDTO> getBudgetsByCategory(Long categoryId);
    List<BudgetDTO> getGeneralBudgets();
    List<BudgetDTO> getOverBudgets();
    List<BudgetDTO> getBudgetsByPeriodOrderByVariance(Long periodId);

    void updateActualValuesForPeriod(Long periodId);
    void updateActualValuesForBudget(Long budgetId);

    void updateBudgetWithRealMovements(Long budgetId);

    void updateAllBudgetsWithRealMovements();
}