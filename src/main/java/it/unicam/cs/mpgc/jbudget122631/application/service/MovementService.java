package it.unicam.cs.mpgc.jbudget122631.application.service;

import it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Movement;
import it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Period;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovementService {

    MovementDTO createMovement(MovementDTO movementDTO);
    Optional<MovementDTO> getMovementById(Long id);
    List<MovementDTO> getAllMovements();
    MovementDTO updateMovement(Long id, MovementDTO movementDTO);
    void deleteMovement(Long id);

    List<MovementDTO> getMovementsByDateRange(LocalDate startDate, LocalDate endDate);
    List<MovementDTO> getMovementsByType(MovementType type);
    List<MovementDTO> getMovementsByCategory(Long categoryId);
    List<MovementDTO> getMovementsByPeriod(Long periodId);

    BigDecimal getTotalByTypeAndDateRange(MovementType type, LocalDate startDate, LocalDate endDate);
    BigDecimal getTotalByCategoryAndDateRange(Long categoryId, LocalDate startDate, LocalDate endDate);

    List<MovementDTO> searchMovements(String searchTerm);

    List<MovementDTO> getMovementsPaginated(int page, int size);
    long getTotalMovementsCount();
    Movement createMovement(Movement movement);
}