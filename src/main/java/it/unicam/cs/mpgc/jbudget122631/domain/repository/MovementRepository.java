package it.unicam.cs.mpgc.jbudget122631.domain.repository;

import it.unicam.cs.mpgc.jbudget122631.domain.model.Movement;
import it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovementRepository {

    // CRUD operations
    Movement save(Movement movement);
    Optional<Movement> findById(Long id);
    List<Movement> findAll();
    void delete(Movement movement);
    void deleteById(Long id);

    // Query methods
    List<Movement> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<Movement> findByType(MovementType type);
    List<Movement> findByCategory(Category category);
    List<Movement> findByCategoriesContaining(Category category);
    List<Movement> findByPeriod(Period period);

    // Statistics queries
    BigDecimal getTotalByTypeAndDateRange(MovementType type, LocalDate startDate, LocalDate endDate);
    BigDecimal getTotalByCategoryAndDateRange(Category category, LocalDate startDate, LocalDate endDate);
    List<Movement> findByDescriptionContaining(String description);

    // Scheduled movements
    List<Movement> findScheduledMovements();
    List<Movement> findByAmortizationPlanId(Long planId);

    // Pagination
    List<Movement> findAllPaginated(int page, int size);
    long count();
}
