package it.unicam.cs.mpgc.jbudget122631.application.usecase;

import it.unicam.cs.mpgc.jbudget122631.application.dto.BudgetDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.BudgetService;

public class CreateBudgetUseCase {

    private final BudgetService budgetService;

    public CreateBudgetUseCase(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    public BudgetDTO execute(BudgetDTO budgetDTO) {
        validateBudgetData(budgetDTO);

        // Verifica che non esista già un budget per periodo/categoria
        if (budgetService.getBudgetByPeriodAndCategory(budgetDTO.getPeriodId(),
                budgetDTO.getCategoryId()).isPresent()) {
            throw new IllegalArgumentException("Budget gia' esistente per questo periodo/categoria");
        }

        return budgetService.createBudget(budgetDTO);
    }

    private void validateBudgetData(BudgetDTO budgetDTO) {
        if (budgetDTO.getPeriodId() == null) {
            throw new IllegalArgumentException("Periodo richiesto per il budget");
        }
        if (budgetDTO.getPlannedIncome() != null && budgetDTO.getPlannedIncome().signum() < 0) {
            throw new IllegalArgumentException("Entrate pianificate devono essere >= 0");
        }
        if (budgetDTO.getPlannedExpenses() != null && budgetDTO.getPlannedExpenses().signum() < 0) {
            throw new IllegalArgumentException("Spese pianificate devono essere >= 0");
        }
    }
}