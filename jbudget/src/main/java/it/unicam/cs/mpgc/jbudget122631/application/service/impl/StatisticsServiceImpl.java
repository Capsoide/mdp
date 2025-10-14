package it.unicam.cs.mpgc.jbudget122631.application.service.impl;

import it.unicam.cs.mpgc.jbudget122631.application.dto.StatisticsDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.StatisticsService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.*;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticsServiceImpl implements StatisticsService {

    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Categoria non trovata";
    private static final String PERIOD_NOT_FOUND_MESSAGE = "Periodo non trovato";
    private static final String PERIOD_1_KEY = "period1";
    private static final String PERIOD_2_KEY = "period2";

    // Chiavi per l'analisi delle performance dei budget
    private static final String TOTAL_BUDGETS_KEY = "totalBudgets";
    private static final String OVER_BUDGET_COUNT_KEY = "overBudgetCount";
    private static final String TOTAL_VARIANCE_KEY = "totalVariance";
    private static final String OVER_BUDGET_PERCENTAGE_KEY = "overBudgetPercentage";

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final MovementRepository movementRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    public StatisticsServiceImpl(MovementRepository movementRepository,
                                 BudgetRepository budgetRepository,
                                 CategoryRepository categoryRepository) {
        this.movementRepository = movementRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public StatisticsDTO getStatisticsForPeriod(LocalDate startDate, LocalDate endDate) {
        StatisticsDTO stats = new StatisticsDTO(startDate, endDate);

        // Calcola totali generali usando query ottimizzate
        PeriodTotals totals = calculatePeriodTotals(startDate, endDate);
        populateBasicStatistics(stats, totals);

        // Calcola statistiche dettagliate per categoria
        stats.setIncomeByCategory(calculateIncomeByCategory(startDate, endDate));
        stats.setExpensesByCategory(calculateExpensesByCategory(startDate, endDate));

        // Calcola trend mensile per il periodo
        stats.setMonthlyTrend(calculateMonthlyTrend(startDate, endDate));

        return stats;
    }

    @Override
    public StatisticsDTO getStatisticsForCategory(Long categoryId, LocalDate startDate, LocalDate endDate) {
        Category category = findCategoryById(categoryId);
        StatisticsDTO stats = new StatisticsDTO(startDate, endDate);

        CategoryTotals categoryTotals = calculateCategoryTotals(category, startDate, endDate);
        populateCategoryStatistics(stats, categoryTotals);

        return stats;
    }

    @Override
    public Map<String, StatisticsDTO> comparePeriodsStatistics(LocalDate period1Start, LocalDate period1End,
                                                               LocalDate period2Start, LocalDate period2End) {
        Map<String, StatisticsDTO> comparison = new LinkedHashMap<>();

        comparison.put(PERIOD_1_KEY, getStatisticsForPeriod(period1Start, period1End));
        comparison.put(PERIOD_2_KEY, getStatisticsForPeriod(period2Start, period2End));

        return comparison;
    }

    @Override
    public Map<String, BigDecimal> getMonthlyIncomeExpensesTrend(LocalDate startDate, LocalDate endDate) {
        return calculateMonthlyTrend(startDate, endDate);
    }

    @Override
    public Map<String, BigDecimal> getCategorySpendingTrend(Long categoryId, LocalDate startDate, LocalDate endDate) {
        Category category = findCategoryById(categoryId);
        return calculateCategoryMonthlyTrend(category, startDate, endDate);
    }

    @Override
    public Map<String, Object> getBudgetPerformanceAnalysis(Long periodId) {
        Period period = findPeriodFromBudgets(periodId);
        List<Budget> budgets = budgetRepository.findByPeriod(period);

        return createBudgetAnalysis(budgets);
    }

    @Override
    public Map<String, BigDecimal> getTopSpendingCategories(LocalDate startDate, LocalDate endDate, int limit) {
        List<Category> activeCategories = categoryRepository.findActiveCategories();

        return activeCategories.stream()
                .collect(Collectors.toMap(
                        Category::getName,
                        cat -> calculateCategoryExpenses(cat, startDate, endDate)
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException(CATEGORY_NOT_FOUND_MESSAGE + " con ID: " + categoryId));
    }

    private Period findPeriodFromBudgets(Long periodId) {
        return budgetRepository.findById(periodId)
                .map(Budget::getPeriod)
                .orElseThrow(() -> new RuntimeException(PERIOD_NOT_FOUND_MESSAGE + " con ID: " + periodId));
    }

    private PeriodTotals calculatePeriodTotals(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalIncome = movementRepository.getTotalByTypeAndDateRange(
                MovementType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = movementRepository.getTotalByTypeAndDateRange(
                MovementType.EXPENSE, startDate, endDate);

        return new PeriodTotals(totalIncome, totalExpenses);
    }

    private CategoryTotals calculateCategoryTotals(Category category, LocalDate startDate, LocalDate endDate) {
        List<Movement> movements = getMovementsInDateRange(category, startDate, endDate);

        BigDecimal income = sumMovementsByType(movements, true);
        BigDecimal expenses = sumMovementsByType(movements, false);

        return new CategoryTotals(income, expenses);
    }

    private List<Movement> getMovementsInDateRange(Category category, LocalDate startDate, LocalDate endDate) {
        return movementRepository.findByCategory(category)
                .stream()
                .filter(m -> isMovementInDateRange(m, startDate, endDate))
                .collect(Collectors.toList());
    }

    private boolean isMovementInDateRange(Movement movement, LocalDate startDate, LocalDate endDate) {
        LocalDate movementDate = movement.getDate();
        return !movementDate.isBefore(startDate) && !movementDate.isAfter(endDate);
    }

    private BigDecimal sumMovementsByType(List<Movement> movements, boolean isIncome) {
        return movements.stream()
                .filter(m -> isIncome ? m.isIncome() : m.isExpense())
                .map(Movement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void populateBasicStatistics(StatisticsDTO stats, PeriodTotals totals) {
        stats.setTotalIncome(totals.income);
        stats.setTotalExpenses(totals.expenses);
        stats.setBalance(totals.income.subtract(totals.expenses));
    }

    private void populateCategoryStatistics(StatisticsDTO stats, CategoryTotals totals) {
        stats.setTotalIncome(totals.income);
        stats.setTotalExpenses(totals.expenses);
        stats.setBalance(totals.income.subtract(totals.expenses));
    }

    private Map<String, BigDecimal> calculateIncomeByCategory(LocalDate startDate, LocalDate endDate) {
        return calculateAmountsByCategory(startDate, endDate, true);
    }

    private Map<String, BigDecimal> calculateExpensesByCategory(LocalDate startDate, LocalDate endDate) {
        return calculateAmountsByCategory(startDate, endDate, false);
    }

    private Map<String, BigDecimal> calculateAmountsByCategory(LocalDate startDate, LocalDate endDate, boolean isIncome) {
        List<Category> activeCategories = categoryRepository.findActiveCategories();
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (Category category : activeCategories) {
            BigDecimal total = calculateCategoryAmountByType(category, startDate, endDate, isIncome);

            if (total.compareTo(BigDecimal.ZERO) > 0) {
                result.put(category.getName(), total);
            }
        }

        return result;
    }

    private BigDecimal calculateCategoryAmountByType(Category category, LocalDate startDate, LocalDate endDate, boolean isIncome) {
        List<Movement> movements = getMovementsInDateRange(category, startDate, endDate);
        return sumMovementsByType(movements, isIncome);
    }

    private BigDecimal calculateCategoryExpenses(Category category, LocalDate startDate, LocalDate endDate) {
        return calculateCategoryAmountByType(category, startDate, endDate, false);
    }

    private Map<String, BigDecimal> calculateMonthlyTrend(LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> monthlyTrend = new LinkedHashMap<>();

        processMonthsInRange(startDate, endDate, (monthStart, monthEnd, yearMonth) -> {
            BigDecimal monthlyBalance = calculateMonthlyBalance(monthStart, monthEnd);
            monthlyTrend.put(yearMonth.toString(), monthlyBalance);
        });

        return monthlyTrend;
    }

    private Map<String, BigDecimal> calculateCategoryMonthlyTrend(Category category, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();

        processMonthsInRange(startDate, endDate, (monthStart, monthEnd, yearMonth) -> {
            BigDecimal monthlyTotal = movementRepository.getTotalByCategoryAndDateRange(category, monthStart, monthEnd);
            trend.put(yearMonth.toString(), monthlyTotal);
        });

        return trend;
    }

    private void processMonthsInRange(LocalDate startDate, LocalDate endDate, MonthProcessor processor) {
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);

        YearMonth current = start;
        while (!current.isAfter(end)) {
            DateRange monthRange = calculateMonthRange(current, startDate, endDate);
            processor.process(monthRange.start, monthRange.end, current);
            current = current.plusMonths(1);
        }
    }

    private DateRange calculateMonthRange(YearMonth yearMonth, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // Adatta alle date del periodo se necessario
        if (monthStart.isBefore(periodStart)) monthStart = periodStart;
        if (monthEnd.isAfter(periodEnd)) monthEnd = periodEnd;

        return new DateRange(monthStart, monthEnd);
    }

    private BigDecimal calculateMonthlyBalance(LocalDate monthStart, LocalDate monthEnd) {
        BigDecimal monthlyIncome = movementRepository.getTotalByTypeAndDateRange(
                MovementType.INCOME, monthStart, monthEnd);
        BigDecimal monthlyExpenses = movementRepository.getTotalByTypeAndDateRange(
                MovementType.EXPENSE, monthStart, monthEnd);

        return monthlyIncome.subtract(monthlyExpenses);
    }

    private Map<String, Object> createBudgetAnalysis(List<Budget> budgets) {
        Map<String, Object> analysis = new LinkedHashMap<>();

        BudgetMetrics metrics = calculateBudgetMetrics(budgets);

        analysis.put(TOTAL_BUDGETS_KEY, budgets.size());
        analysis.put(OVER_BUDGET_COUNT_KEY, metrics.overBudgetCount);
        analysis.put(TOTAL_VARIANCE_KEY, metrics.totalVariance);
        analysis.put(OVER_BUDGET_PERCENTAGE_KEY, metrics.overBudgetPercentage);

        return analysis;
    }

    private BudgetMetrics calculateBudgetMetrics(List<Budget> budgets) {
        if (budgets.isEmpty()) {
            return new BudgetMetrics(0L, BigDecimal.ZERO, 0.0);
        }

        long overBudgetCount = budgets.stream()
                .mapToLong(b -> b.isOverBudget() ? 1 : 0)
                .sum();

        BigDecimal totalVariance = budgets.stream()
                .map(Budget::getVarianceBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double overBudgetPercentage = calculatePercentage(overBudgetCount, budgets.size());

        return new BudgetMetrics(overBudgetCount, totalVariance, overBudgetPercentage);
    }

    private double calculatePercentage(long numerator, int denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @FunctionalInterface
    private interface MonthProcessor {
        void process(LocalDate monthStart, LocalDate monthEnd, YearMonth yearMonth);
    }

    private static class DateRange {
        final LocalDate start;
        final LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class PeriodTotals {
        final BigDecimal income;
        final BigDecimal expenses;

        PeriodTotals(BigDecimal income, BigDecimal expenses) {
            this.income = income != null ? income : BigDecimal.ZERO;
            this.expenses = expenses != null ? expenses : BigDecimal.ZERO;
        }
    }

    private static class CategoryTotals {
        final BigDecimal income;
        final BigDecimal expenses;

        CategoryTotals(BigDecimal income, BigDecimal expenses) {
            this.income = income;
            this.expenses = expenses;
        }
    }

    private static class BudgetMetrics {
        final long overBudgetCount;
        final BigDecimal totalVariance;
        final double overBudgetPercentage;

        BudgetMetrics(long overBudgetCount, BigDecimal totalVariance, double overBudgetPercentage) {
            this.overBudgetCount = overBudgetCount;
            this.totalVariance = totalVariance;
            this.overBudgetPercentage = overBudgetPercentage;
        }
    }
}