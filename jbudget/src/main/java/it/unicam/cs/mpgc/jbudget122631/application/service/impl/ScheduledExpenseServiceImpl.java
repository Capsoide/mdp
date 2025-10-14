package it.unicam.cs.mpgc.jbudget122631.application.service.impl;

import it.unicam.cs.mpgc.jbudget122631.application.service.ScheduledExpenseService;
import it.unicam.cs.mpgc.jbudget122631.application.service.MovementService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.ScheduledExpense;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Movement;
import it.unicam.cs.mpgc.jbudget122631.domain.model.RecurrenceType;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.ScheduledExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScheduledExpenseServiceImpl implements ScheduledExpenseService {

    private static final int DEFAULT_ATTENTION_DAYS = 3;
    private static final String EXPENSE_NOT_FOUND_MESSAGE = "Spesa programmata non trovata";
    private static final String EXPENSE_ALREADY_COMPLETED_MESSAGE = "Spesa già completata";

    private final ScheduledExpenseRepository scheduledExpenseRepository;
    private final MovementService movementService;

    public ScheduledExpenseServiceImpl(ScheduledExpenseRepository scheduledExpenseRepository,
                                       MovementService movementService) {
        this.scheduledExpenseRepository = scheduledExpenseRepository;
        this.movementService = movementService;
    }

    @Override
    public ScheduledExpense createScheduledExpense(ScheduledExpense scheduledExpense) {
        validateScheduledExpense(scheduledExpense);
        return scheduledExpenseRepository.save(scheduledExpense);
    }

    @Override
    public Optional<ScheduledExpense> getScheduledExpenseById(Long id) {
        return scheduledExpenseRepository.findById(id);
    }

    @Override
    public List<ScheduledExpense> getAllScheduledExpenses() {
        return scheduledExpenseRepository.findAll();
    }

    @Override
    public ScheduledExpense updateScheduledExpense(Long id, ScheduledExpense updatedExpense) {
        ScheduledExpense existing = findScheduledExpenseById(id);

        validateScheduledExpense(updatedExpense);
        updateExpenseFields(existing, updatedExpense);

        return scheduledExpenseRepository.save(existing);
    }

    @Override
    public void deleteScheduledExpense(Long id) {
        scheduledExpenseRepository.deleteById(id);
    }

    @Override
    public List<ScheduledExpense> getDueExpenses() {
        return scheduledExpenseRepository.findByCompleted(false)
                .stream()
                .filter(ScheduledExpense::isDue)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScheduledExpense> getOverdueExpenses() {
        return scheduledExpenseRepository.findOverdueExpenses();
    }

    @Override
    public List<ScheduledExpense> getExpensesDueInDays(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return scheduledExpenseRepository.findByDueDateBetween(startDate, endDate);
    }

    @Override
    public Movement completeScheduledExpense(Long id) {
        ScheduledExpense expense = findScheduledExpenseById(id);

        validateExpenseNotCompleted(expense);

        Movement movement = createMovementFromExpense(expense);
        Movement savedMovement = persistMovementViService(movement);

        markExpenseAsCompleted(expense);
        handleRecurrenceIfApplicable(expense);

        return savedMovement;
    }

    @Override
    public void markAsCompleted(Long id) {
        ScheduledExpense expense = findScheduledExpenseById(id);
        expense.setCompleted(true);
        scheduledExpenseRepository.save(expense);
    }

    @Override
    public List<ScheduledExpense> getRecurringExpenses() {
        return scheduledExpenseRepository.findRecurringExpenses();
    }

    @Override
    public ScheduledExpense createNextOccurrence(Long id) {
        ScheduledExpense expense = findScheduledExpenseById(id);
        ScheduledExpense nextOccurrence = expense.createNextOccurrence();
        return scheduledExpenseRepository.save(nextOccurrence);
    }

    @Override
    public void processRecurringExpenses() {
        List<ScheduledExpense> recurringExpenses = getRecurringExpenses();

        System.out.println("RECURRING - Processamento " + recurringExpenses.size() + " spese ricorrenti...");

        int processedCount = 0;
        int errorCount = 0;

        for (ScheduledExpense expense : recurringExpenses) {
            if (shouldCreateNextOccurrence(expense)) {
                try {
                    createNextOccurrence(expense.getId());
                    processedCount++;
                    System.out.println("RECURRING - Creata prossima occorrenza per: " + expense.getDescription());
                } catch (Exception e) {
                    errorCount++;
                    logRecurrenceError(expense, e);
                }
            }
        }

        System.out.println("RECURRING - Processamento completato: " +
                processedCount + " successi, " + errorCount + " errori");
    }

    @Override
    public List<ScheduledExpense> getExpensesRequiringAttention() {
        List<ScheduledExpense> attentionList = new ArrayList<>();

        // Aggiungi spese scadute
        attentionList.addAll(getOverdueExpenses());

        // Aggiungi spese in scadenza nei prossimi giorni
        attentionList.addAll(getExpensesDueInDays(DEFAULT_ATTENTION_DAYS));

        // Rimuovi duplicati e restituisci lista pulita
        return attentionList.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private ScheduledExpense findScheduledExpenseById(Long id) {
        return scheduledExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(EXPENSE_NOT_FOUND_MESSAGE + " con ID: " + id));
    }

    private void updateExpenseFields(ScheduledExpense existing, ScheduledExpense updated) {
        existing.setDescription(updated.getDescription());
        existing.setAmount(updated.getAmount());
        existing.setType(updated.getType());
        existing.setDueDate(updated.getDueDate());
        existing.setRecurrenceType(updated.getRecurrenceType());
        existing.setRecurrenceInterval(updated.getRecurrenceInterval());
        existing.setRecurrenceEndDate(updated.getRecurrenceEndDate());
        existing.setNotes(updated.getNotes());
    }

    private void validateExpenseNotCompleted(ScheduledExpense expense) {
        if (expense.isCompleted()) {
            throw new IllegalStateException(EXPENSE_ALREADY_COMPLETED_MESSAGE);
        }
    }

    private Movement createMovementFromExpense(ScheduledExpense expense) {
        return expense.createMovement();
    }

    private Movement persistMovementViService(Movement movement) {
        return movementService.createMovement(movement);
    }

    private void markExpenseAsCompleted(ScheduledExpense expense) {
        scheduledExpenseRepository.save(expense);
    }

    private void handleRecurrenceIfApplicable(ScheduledExpense expense) {
        if (!expense.isRecurring()) {
            return;
        }

        try {
            ScheduledExpense nextOccurrence = expense.createNextOccurrence();
            scheduledExpenseRepository.save(nextOccurrence);
            System.out.println("RECURRING - Creata prossima occorrenza per: " + expense.getDescription());
        } catch (IllegalStateException e) {
            // Fine ricorrenza raggiunta - comportamento normale
            System.out.println("RECURRING - Fine ricorrenza per spesa: " + expense.getDescription());
        }
    }

    private boolean shouldCreateNextOccurrence(ScheduledExpense expense) {
        return expense.isCompleted() && expense.getNextDueDate() != null;
    }

    private void logRecurrenceError(ScheduledExpense expense, Exception e) {
        System.err.println("RECURRING - Errore creazione prossima occorrenza per spesa " +
                expense.getId() + ": " + e.getMessage());
    }

    private void validateScheduledExpense(ScheduledExpense expense) {
        validateDescription(expense.getDescription());
        validateAmount(expense.getAmount());
        validateDueDate(expense.getDueDate());
        validateRecurrence(expense);
    }

    private void validateDescription(String description) {
        if (isNullOrEmpty(description)) {
            throw new IllegalArgumentException("Descrizione spesa programmata richiesta");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Importo deve essere maggiore di zero");
        }
    }

    private void validateDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Data scadenza richiesta");
        }
    }

    private void validateRecurrence(ScheduledExpense expense) {
        if (isRecurring(expense) && hasInvalidRecurrenceInterval(expense)) {
            throw new IllegalArgumentException("Intervallo ricorrenza deve essere maggiore di zero");
        }
    }

    private boolean isRecurring(ScheduledExpense expense) {
        return expense.getRecurrenceType() != RecurrenceType.NONE;
    }

    private boolean hasInvalidRecurrenceInterval(ScheduledExpense expense) {
        Integer interval = expense.getRecurrenceInterval();
        return interval != null && interval <= 0;
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}