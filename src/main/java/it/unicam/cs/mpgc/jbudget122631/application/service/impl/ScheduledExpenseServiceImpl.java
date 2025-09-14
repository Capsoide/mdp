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

/**
 * Implementazione del servizio per la gestione delle spese programmate e ricorrenti.
 *
 * Gestisce la logica di business per:
 * - Creazione, modifica ed eliminazione di spese programmate
 * - Gestione delle ricorrenze (giornaliere, settimanali, mensili, annuali)
 * - Completamento automatico con creazione di movimenti
 * - Monitoraggio scadenze e spese in ritardo
 * - Processamento batch delle ricorrenze
 *
 * Caratteristiche principali:
 * - Validazione completa dei dati di input
 * - Creazione automatica di prossime occorrenze per spese ricorrenti
 * - Integrazione con MovementService per persistenza movimenti
 * - Sistema di allerte per spese in scadenza o scadute
 * - Gestione intelligente del ciclo di vita delle ricorrenze
 *
 * @author Nicola Capancioni
 * @version 1.0
 */
public class ScheduledExpenseServiceImpl implements ScheduledExpenseService {

    private static final int DEFAULT_ATTENTION_DAYS = 3;
    private static final String EXPENSE_NOT_FOUND_MESSAGE = "Spesa programmata non trovata";
    private static final String EXPENSE_ALREADY_COMPLETED_MESSAGE = "Spesa già completata";

    private final ScheduledExpenseRepository scheduledExpenseRepository;
    private final MovementService movementService;

    /**
     * Costruttore per l'iniezione delle dipendenze.
     *
     * @param scheduledExpenseRepository repository per la persistenza delle spese programmate
     * @param movementService servizio per la creazione di movimenti dal completamento spese
     */
    public ScheduledExpenseServiceImpl(ScheduledExpenseRepository scheduledExpenseRepository,
                                       MovementService movementService) {
        this.scheduledExpenseRepository = scheduledExpenseRepository;
        this.movementService = movementService;
    }

    /**
     * Crea una nuova spesa programmata dopo validazione completa.
     *
     * @param scheduledExpense dati della spesa programmata da creare
     * @return spesa programmata salvata con ID assegnato
     * @throws IllegalArgumentException se i dati non sono validi
     */
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

    /**
     * Aggiorna una spesa programmata esistente con nuovi dati.
     *
     * @param id ID della spesa programmata da aggiornare
     * @param updatedExpense nuovi dati della spesa
     * @return spesa programmata aggiornata
     * @throws RuntimeException se la spesa non esiste
     * @throws IllegalArgumentException se i nuovi dati non sono validi
     */
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

    /**
     * Recupera tutte le spese in scadenza (non completate e con data scadenza oggi o passata).
     *
     * @return lista delle spese in scadenza
     */
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

    /**
     * Recupera le spese che scadono nei prossimi N giorni.
     *
     * @param days numero di giorni da considerare dal giorno corrente
     * @return lista delle spese in scadenza nel periodo specificato
     */
    @Override
    public List<ScheduledExpense> getExpensesDueInDays(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return scheduledExpenseRepository.findByDueDateBetween(startDate, endDate);
    }

    /**
     * Completa una spesa programmata creando il movimento corrispondente.
     *
     * Flusso operativo:
     * 1. Trova la spesa programmata per ID
     * 2. Verifica che non sia già completata
     * 3. Crea il movimento tramite MovementService
     * 4. Marca la spesa come completata
     * 5. Se ricorrente, crea la prossima occorrenza
     *
     * @param id ID della spesa programmata da completare
     * @return movimento creato dal completamento
     * @throws RuntimeException se la spesa non esiste
     * @throws IllegalStateException se la spesa è già completata
     */
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

    /**
     * Marca una spesa programmata come completata.
     *
     * @param id ID della spesa programmata da marcare come completata
     * @throws RuntimeException se la spesa non esiste
     */
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

    /**
     * Crea manualmente la prossima occorrenza di una spesa ricorrente.
     *
     * @param id ID della spesa programmata ricorrente
     * @return prossima occorrenza creata e salvata
     * @throws RuntimeException se la spesa non esiste
     * @throws IllegalStateException se non può creare la prossima occorrenza
     */
    @Override
    public ScheduledExpense createNextOccurrence(Long id) {
        ScheduledExpense expense = findScheduledExpenseById(id);
        ScheduledExpense nextOccurrence = expense.createNextOccurrence();
        return scheduledExpenseRepository.save(nextOccurrence);
    }

    /**
     * Processa automaticamente tutte le spese ricorrenti completate
     * creando le prossime occorrenze dove necessario.
     */
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

    /**
     * Recupera tutte le spese che richiedono attenzione dell'utente.
     *
     * Include:
     * - Spese scadute (overdue)
     * - Spese in scadenza nei prossimi 3 giorni
     *
     * @return lista unificata e deduplicata delle spese che richiedono attenzione
     */
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

    /**
     * Trova una spesa programmata per ID o lancia eccezione.
     */
    private ScheduledExpense findScheduledExpenseById(Long id) {
        return scheduledExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(EXPENSE_NOT_FOUND_MESSAGE + " con ID: " + id));
    }

    /**
     * Aggiorna i campi di una spesa programmata con i nuovi valori.
     */
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

    /**
     * Verifica che una spesa programmata non sia già completata.
     */
    private void validateExpenseNotCompleted(ScheduledExpense expense) {
        if (expense.isCompleted()) {
            throw new IllegalStateException(EXPENSE_ALREADY_COMPLETED_MESSAGE);
        }
    }

    /**
     * Crea un movimento dall'entità spesa programmata.
     */
    private Movement createMovementFromExpense(ScheduledExpense expense) {
        return expense.createMovement();
    }

    /**
     * Persiste il movimento tramite il MovementService per garantire
     * la sincronizzazione automatica dei budget.
     */
    private Movement persistMovementViService(Movement movement) {
        return movementService.createMovement(movement);
    }

    /**
     * Marca una spesa programmata come completata e la salva.
     */
    private void markExpenseAsCompleted(ScheduledExpense expense) {
        scheduledExpenseRepository.save(expense);
    }

    /**
     * Gestisce la creazione della prossima ricorrenza se applicabile.
     */
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

    /**
     * Determina se dovrebbe essere creata la prossima occorrenza per una spesa ricorrente.
     */
    private boolean shouldCreateNextOccurrence(ScheduledExpense expense) {
        return expense.isCompleted() && expense.getNextDueDate() != null;
    }

    /**
     * Logga errori durante il processamento delle ricorrenze.
     */
    private void logRecurrenceError(ScheduledExpense expense, Exception e) {
        System.err.println("RECURRING - Errore creazione prossima occorrenza per spesa " +
                expense.getId() + ": " + e.getMessage());
    }

    /**
     * Valida una spesa programmata secondo le regole di business.
     *
     * Regole di validazione:
     * - Descrizione obbligatoria e non vuota
     * - Importo positivo maggiore di zero
     * - Data scadenza obbligatoria
     * - Se ricorrente, intervallo deve essere positivo
     *
     * @param expense spesa programmata da validare
     * @throws IllegalArgumentException se la validazione fallisce
     */
    private void validateScheduledExpense(ScheduledExpense expense) {
        validateDescription(expense.getDescription());
        validateAmount(expense.getAmount());
        validateDueDate(expense.getDueDate());
        validateRecurrence(expense);
    }

    /**
     * Valida la descrizione della spesa programmata.
     */
    private void validateDescription(String description) {
        if (isNullOrEmpty(description)) {
            throw new IllegalArgumentException("Descrizione spesa programmata richiesta");
        }
    }

    /**
     * Valida l'importo della spesa programmata.
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Importo deve essere maggiore di zero");
        }
    }

    /**
     * Valida la data di scadenza della spesa programmata.
     */
    private void validateDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Data scadenza richiesta");
        }
    }

    /**
     * Valida i parametri di ricorrenza se la spesa è ricorrente.
     */
    private void validateRecurrence(ScheduledExpense expense) {
        if (isRecurring(expense) && hasInvalidRecurrenceInterval(expense)) {
            throw new IllegalArgumentException("Intervallo ricorrenza deve essere maggiore di zero");
        }
    }

    /**
     * Verifica se una spesa ha impostazioni di ricorrenza.
     */
    private boolean isRecurring(ScheduledExpense expense) {
        return expense.getRecurrenceType() != RecurrenceType.NONE;
    }

    /**
     * Verifica se l'intervallo di ricorrenza è invalido.
     */
    private boolean hasInvalidRecurrenceInterval(ScheduledExpense expense) {
        Integer interval = expense.getRecurrenceInterval();
        return interval != null && interval <= 0;
    }

    /**
     * Verifica se una stringa è null o vuota dopo trim.
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}