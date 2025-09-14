package it.unicam.cs.mpgc.jbudget122631.application.service;

import it.unicam.cs.mpgc.jbudget122631.application.dto.StatisticsDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface StatisticsService {

    // General statistics
    StatisticsDTO getStatisticsForPeriod(LocalDate startDate, LocalDate endDate);
    StatisticsDTO getStatisticsForCategory(Long categoryId, LocalDate startDate, LocalDate endDate);

    // Comparative analysis
    Map<String, StatisticsDTO> comparePeriodsStatistics(LocalDate period1Start, LocalDate period1End,
                                                        LocalDate period2Start, LocalDate period2End);

    // Trends
    Map<String, BigDecimal> getMonthlyIncomeExpensesTrend(LocalDate startDate, LocalDate endDate);
    Map<String, BigDecimal> getCategorySpendingTrend(Long categoryId, LocalDate startDate, LocalDate endDate);

    // Budget analysis
    Map<String, Object> getBudgetPerformanceAnalysis(Long periodId);

    // Top spending categories
    Map<String, BigDecimal> getTopSpendingCategories(LocalDate startDate, LocalDate endDate, int limit);
}