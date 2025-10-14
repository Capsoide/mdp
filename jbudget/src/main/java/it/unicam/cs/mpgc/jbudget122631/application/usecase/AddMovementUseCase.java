package it.unicam.cs.mpgc.jbudget122631.application.usecase;

import it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.MovementService;
import it.unicam.cs.mpgc.jbudget122631.application.service.BudgetService;

public class AddMovementUseCase {

    private final MovementService movementService;
    private final BudgetService budgetService;

    public AddMovementUseCase(MovementService movementService, BudgetService budgetService) {
        this.movementService = movementService;
        this.budgetService = budgetService;
    }

    public MovementDTO execute(MovementDTO movementDTO) {
        // Valida i dati
        validateMovementData(movementDTO);

        // Crea il movimento
        MovementDTO createdMovement = movementService.createMovement(movementDTO);

        // Aggiorna i budget correlati
        updateRelatedBudgets(createdMovement);

        return createdMovement;
    }

    private void validateMovementData(MovementDTO movementDTO) {
        if (movementDTO.getDescription() == null || movementDTO.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrizione movimento richiesta");
        }
        if (movementDTO.getAmount() == null || movementDTO.getAmount().signum() < 0) {
            throw new IllegalArgumentException("Importo deve essere >= 0");
        }
        if (movementDTO.getType() == null) {
            throw new IllegalArgumentException("Tipo movimento richiesto");
        }
        if (movementDTO.getDate() == null) {
            throw new IllegalArgumentException("Data movimento richiesta");
        }
    }

    private void updateRelatedBudgets(MovementDTO movement) {
        // Trova periodo contenente la data del movimento
        // Aggiorna budget correlati (implementazione dettagliata nel BudgetService)
        // Questo è un trigger per mantenere sincronizzati i valori actual nei budget
    }
}