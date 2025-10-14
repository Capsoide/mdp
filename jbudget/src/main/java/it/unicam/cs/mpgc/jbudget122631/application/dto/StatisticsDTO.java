package it.unicam.cs.mpgc.jbudget122631.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StatisticsDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal balance;
    private Map<String, BigDecimal> incomeByCategory;
    private Map<String, BigDecimal> expensesByCategory;
    private Map<String, BigDecimal> monthlyTrend;

    public StatisticsDTO() {
        this.totalIncome = BigDecimal.ZERO;
        this.totalExpenses = BigDecimal.ZERO;
        this.balance = BigDecimal.ZERO;
        this.incomeByCategory = new HashMap<>();
        this.expensesByCategory = new HashMap<>();
        this.monthlyTrend = new HashMap<>();
    }

    public StatisticsDTO(LocalDate startDate, LocalDate endDate) {
        this();
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private BigDecimal nullSafeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
    private Map<String, BigDecimal> nullSafeMap(Map<String, BigDecimal> map) {
        return map != null ? map : new HashMap<>();
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    public BigDecimal getTotalIncome() {
        return nullSafeBigDecimal(totalIncome);
    }
    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = nullSafeBigDecimal(totalIncome);
    }
    public BigDecimal getTotalExpenses() {
        return nullSafeBigDecimal(totalExpenses);
    }
    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = nullSafeBigDecimal(totalExpenses);
    }
    public BigDecimal getBalance() {
        return nullSafeBigDecimal(balance);
    }
    public void setBalance(BigDecimal balance) {
        this.balance = nullSafeBigDecimal(balance);
    }
    public Map<String, BigDecimal> getIncomeByCategory() {
        return nullSafeMap(incomeByCategory);
    }
    public void setIncomeByCategory(Map<String, BigDecimal> incomeByCategory) {
        this.incomeByCategory = nullSafeMap(incomeByCategory);
    }
    public Map<String, BigDecimal> getExpensesByCategory() {
        return nullSafeMap(expensesByCategory);
    }
    public void setExpensesByCategory(Map<String, BigDecimal> expensesByCategory) {
        this.expensesByCategory = nullSafeMap(expensesByCategory);
    }
    public Map<String, BigDecimal> getMonthlyTrend() {
        return nullSafeMap(monthlyTrend);
    }
    public void setMonthlyTrend(Map<String, BigDecimal> monthlyTrend) {
        this.monthlyTrend = nullSafeMap(monthlyTrend);
    }
    public void calculateBalance() {
        this.balance = getTotalIncome().subtract(getTotalExpenses());
    }

    public boolean isValidPeriod() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    public boolean hasMovements() {
        return getTotalIncome().compareTo(BigDecimal.ZERO) > 0 ||
                getTotalExpenses().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatisticsDTO)) return false;
        StatisticsDTO that = (StatisticsDTO) o;
        return Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                Objects.equals(getTotalIncome(), that.getTotalIncome()) &&
                Objects.equals(getTotalExpenses(), that.getTotalExpenses());
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate, getTotalIncome(), getTotalExpenses());
    }

    @Override
    public String toString() {
        return String.format("StatisticsDTO{startDate=%s, endDate=%s, totalIncome=%s, totalExpenses=%s, balance=%s}",
                Objects.toString(startDate, "null"),
                Objects.toString(endDate, "null"),
                Objects.toString(getTotalIncome(), "0"),
                Objects.toString(getTotalExpenses(), "0"),
                Objects.toString(getBalance(), "0"));
    }
}