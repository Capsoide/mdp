package it.unicam.cs.mpgc.jbudget122631.application.service;

import it.unicam.cs.mpgc.jbudget122631.application.dto.StatisticsDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface StatisticsService {

    StatisticsDTO getStatisticsForPeriod(LocalDate startDate, LocalDate endDate);
    StatisticsDTO getStatisticsForCategory(Long categoryId, LocalDate startDate, LocalDate endDate);

    Map<String, StatisticsDTO> comparePeriodsStatistics(LocalDate period1Start, LocalDate period1End,
                                                        LocalDate period2Start, LocalDate period2End);

    Map<String, BigDecimal> getMonthlyIncomeExpensesTrend(LocalDate startDate, LocalDate endDate);
    Map<String, BigDecimal> getCategorySpendingTrend(Long categoryId, LocalDate startDate, LocalDate endDate);

    Map<String, Object> getBudgetPerformanceAnalysis(Long periodId);

    Map<String, BigDecimal> getTopSpendingCategories(LocalDate startDate, LocalDate endDate, int limit);
}