package it.unicam.cs.mpgc.jbudget122631.application.usecase;

import it.unicam.cs.mpgc.jbudget122631.application.dto.StatisticsDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.StatisticsService;

import java.time.LocalDate;

public class GenerateStatisticsUseCase {

    private final StatisticsService statisticsService;

    public GenerateStatisticsUseCase(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    public StatisticsDTO execute(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return statisticsService.getStatisticsForPeriod(startDate, endDate);
    }

    public StatisticsDTO executeForCategory(Long categoryId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        if (categoryId == null) {
            throw new IllegalArgumentException("ID categoria richiesto");
        }
        return statisticsService.getStatisticsForCategory(categoryId, startDate, endDate);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Date inizio e fine richieste");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Data inizio deve essere <= data fine");
        }
    }
}