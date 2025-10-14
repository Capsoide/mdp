package it.unicam.cs.mpgc.jbudget122631.domain.repository;

import it.unicam.cs.mpgc.jbudget122631.domain.model.Period;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodRepository {

    Period save(Period period);
    Optional<Period> findById(Long id);
    List<Period> findAll();
    void delete(Period period);
    void deleteById(Long id);

    Optional<Period> findByName(String name);
    List<Period> findByDateRange(LocalDate startDate, LocalDate endDate);
    Optional<Period> findPeriodContaining(LocalDate date);
    List<Period> findOverlappingPeriods(LocalDate startDate, LocalDate endDate);

    List<Period> findCurrentPeriods();
    List<Period> findFuturePeriods();
    List<Period> findPastPeriods();

    long count();
}