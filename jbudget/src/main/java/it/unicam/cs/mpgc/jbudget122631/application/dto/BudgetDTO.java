package it.unicam.cs.mpgc.jbudget122631.application.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class BudgetDTO {
    private Long id;
    private Long periodId;
    private String periodName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal plannedIncome;
    private BigDecimal plannedExpenses;
    private BigDecimal actualIncome;
    private BigDecimal actualExpenses;
    private String notes;

    public BudgetDTO() {
        this.plannedIncome = BigDecimal.ZERO;
        this.plannedExpenses = BigDecimal.ZERO;
        this.actualIncome = BigDecimal.ZERO;
        this.actualExpenses = BigDecimal.ZERO;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double calculatePercentage(BigDecimal actual, BigDecimal planned) {
        if (planned.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return actual.divide(planned, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }
    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }

    public Long getCategoryId() { return categoryId; }

    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getPlannedIncome() {
        return nullSafe(plannedIncome);
    }
    public void setPlannedIncome(BigDecimal plannedIncome) {
        this.plannedIncome = nullSafe(plannedIncome);
    }
    public BigDecimal getPlannedExpenses() {
        return nullSafe(plannedExpenses);
    }
    public void setPlannedExpenses(BigDecimal plannedExpenses) {
        this.plannedExpenses = nullSafe(plannedExpenses);
    }
    public BigDecimal getActualIncome() {
        return nullSafe(actualIncome);
    }
    public void setActualIncome(BigDecimal actualIncome) {
        this.actualIncome = nullSafe(actualIncome);
    }
    public BigDecimal getActualExpenses() {
        return nullSafe(actualExpenses);
    }
    public void setActualExpenses(BigDecimal actualExpenses) {
        this.actualExpenses = nullSafe(actualExpenses);
    }
    public BigDecimal getPlannedBalance() {
        return getPlannedIncome().subtract(getPlannedExpenses());
    }
    public BigDecimal getActualBalance() {
        return getActualIncome().subtract(getActualExpenses());
    }
    public BigDecimal getVarianceBalance() {
        return getActualBalance().subtract(getPlannedBalance());
    }
    public double getIncomePercentage() {
        return calculatePercentage(getActualIncome(), getPlannedIncome());
    }
    public double getExpensesPercentage() {
        return calculatePercentage(getActualExpenses(), getPlannedExpenses());
    }
    public boolean isOverBudget() {
        return getActualExpenses().compareTo(getPlannedExpenses()) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BudgetDTO)) return false;
        BudgetDTO budgetDTO = (BudgetDTO) o;
        return Objects.equals(id, budgetDTO.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}