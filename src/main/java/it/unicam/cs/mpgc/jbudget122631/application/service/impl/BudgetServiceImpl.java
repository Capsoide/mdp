package it.unicam.cs.mpgc.jbudget122631.application.service.impl;

import it.unicam.cs.mpgc.jbudget122631.application.dto.BudgetDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.BudgetService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.*;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementazione del servizio per la gestione dei budget familiari.
 *
 * Questa classe implementa la logica di business per:
 * - Creazione, modifica ed eliminazione di budget
 * - Calcolo automatico dei valori reali basati sui movimenti
 * - Gestione delle associazioni con periodi e categorie
 *
 * @author Nicola Capancioni
 * @version 1.0
 */
public class BudgetServiceImpl implements BudgetService {

    private static final String GENERAL_CATEGORY_NAME = "Generale";
    private static final String CURRENT_MONTH_FALLBACK = "Mese Corrente";

    private final BudgetRepository budgetRepository;
    private final MovementRepository movementRepository;
    private final PeriodRepository periodRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Costruttore per l'iniezione delle dipendenze dei repository.
     *
     * @param budgetRepository repository per la gestione dei budget
     * @param movementRepository repository per la gestione dei movimenti
     * @param periodRepository repository per la gestione dei periodi
     * @param categoryRepository repository per la gestione delle categorie
     */
    public BudgetServiceImpl(BudgetRepository budgetRepository,
                             MovementRepository movementRepository,
                             PeriodRepository periodRepository,
                             CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.movementRepository = movementRepository;
        this.periodRepository = periodRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public BudgetDTO createBudget(BudgetDTO budgetDTO) {
        Budget budget = convertToEntity(budgetDTO);
        Budget savedBudget = budgetRepository.save(budget);

        // Calcola immediatamente i valori reali basati sui movimenti esistenti
        updateBudgetWithRealMovements(savedBudget.getId());

        return convertToDTO(budgetRepository.findById(savedBudget.getId()).orElse(savedBudget));
    }

    @Override
    public Optional<BudgetDTO> getBudgetById(Long id) {
        return budgetRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public List<BudgetDTO> getAllBudgets() {
        return budgetRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BudgetDTO updateBudget(Long id, BudgetDTO budgetDTO) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget non trovato"));

        // Aggiorna solo i valori pianificati (i valori reali vengono ricalcolati)
        budget.setPlannedIncome(budgetDTO.getPlannedIncome());
        budget.setPlannedExpenses(budgetDTO.getPlannedExpenses());
        budget.setNotes(budgetDTO.getNotes());

        Budget savedBudget = budgetRepository.save(budget);

        // Ricalcola automaticamente i valori reali
        updateBudgetWithRealMovements(savedBudget.getId());

        return convertToDTO(budgetRepository.findById(savedBudget.getId()).orElse(savedBudget));
    }

    @Override
    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }

    /**
     * Aggiorna i valori reali di un budget specifico basandosi sui movimenti del periodo.
     *
     * Logica di calcolo:
     * - Entrate (INCOME): vengono distribuite su tutti i budget del periodo
     * - Spese (EXPENSE): vengono filtrate per categoria specifica o considerate tutte per budget generali
     *
     * @param budgetId ID del budget da aggiornare
     */
    @Override
    public void updateBudgetWithRealMovements(Long budgetId) {
        try {
            System.out.println("BUDGET - Aggiornamento valori reali per budget ID: " + budgetId);

            Budget budget = budgetRepository.findById(budgetId)
                    .orElseThrow(() -> new RuntimeException("Budget non trovato"));

            Period period = budget.getPeriod();
            Category category = budget.getCategory();

            logBudgetProcessingInfo(period, category);

            List<Movement> movements = movementRepository.findByDateBetween(
                    period.getStartDate(), period.getEndDate());

            System.out.println("BUDGET - Movimenti trovati nel periodo " + period.getName() + ": " + movements.size());

            // Calcola le entrate totali (distribuite su tutti i budget del periodo)
            BigDecimal actualIncome = calculateTotalIncome(movements);

            // Calcola le spese filtrate per categoria
            BigDecimal actualExpenses = calculateExpensesForCategory(movements, category);

            logCalculationDetails(movements, category, actualIncome, actualExpenses);

            // Aggiorna e salva il budget
            updateAndSaveBudget(budget, actualIncome, actualExpenses, budgetId);

        } catch (Exception e) {
            System.err.println("BUDGET - Errore aggiornamento budget ID " + budgetId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Errore aggiornamento budget", e);
        }
    }

    @Override
    public void updateAllBudgetsWithRealMovements() {
        try {
            System.out.println("BUDGET - Avvio aggiornamento di tutti i budget...");

            List<Budget> allBudgets = budgetRepository.findAll();
            System.out.println("BUDGET - Budget da aggiornare: " + allBudgets.size());

            for (Budget budget : allBudgets) {
                try {
                    updateBudgetWithRealMovements(budget.getId());
                } catch (Exception e) {
                    System.err.println("BUDGET - Errore aggiornamento budget ID " + budget.getId() +
                            ": " + e.getMessage());
                    // Continua con gli altri budget anche se uno fallisce
                }
            }

            System.out.println("BUDGET - Aggiornamento globale completato");

        } catch (Exception e) {
            System.err.println("BUDGET - Errore aggiornamento globale: " + e.getMessage());
            throw new RuntimeException("Errore aggiornamento globale budget", e);
        }
    }

    @Override
    public void updateActualValuesForPeriod(Long periodId) {
        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Periodo non trovato"));

        List<Budget> budgets = budgetRepository.findByPeriod(period);
        for (Budget budget : budgets) {
            updateBudgetWithRealMovements(budget.getId());
        }
    }

    @Override
    public void updateActualValuesForBudget(Long budgetId) {
        updateBudgetWithRealMovements(budgetId);
    }

    @Override
    public Optional<BudgetDTO> getBudgetByPeriodAndCategory(Long periodId, Long categoryId) {
        Period period = periodRepository.findById(periodId).orElse(null);
        Category category = categoryId != null ? categoryRepository.findById(categoryId).orElse(null) : null;

        if (period != null) {
            return budgetRepository.findByPeriodAndCategory(period, category)
                    .map(this::convertToDTO);
        }
        return Optional.empty();
    }

    @Override
    public List<BudgetDTO> getBudgetsByPeriod(Long periodId) {
        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Periodo non trovato"));

        return budgetRepository.findByPeriod(period)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BudgetDTO> getBudgetsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Categoria non trovata"));

        return budgetRepository.findByCategory(category)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BudgetDTO> getGeneralBudgets() {
        return budgetRepository.findGeneralBudgets()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BudgetDTO> getOverBudgets() {
        return budgetRepository.findOverBudgets()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BudgetDTO> getBudgetsByPeriodOrderByVariance(Long periodId) {
        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Periodo non trovato"));

        return budgetRepository.findByPeriodOrderByVariance(period)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converte un DTO in entità Budget.
     *
     * @param dto il DTO da convertire
     * @return l'entità Budget
     */
    private Budget convertToEntity(BudgetDTO dto) {
        Period period = findOrCreatePeriod(dto.getPeriodName());
        Category category = determineCategoryFromDTO(dto);
        return new Budget(period, category, dto.getPlannedIncome(), dto.getPlannedExpenses());
    }

    /**
     * Converte un'entità Budget in DTO.
     *
     * @param budget l'entità da convertire
     * @return il DTO Budget
     */
    private BudgetDTO convertToDTO(Budget budget) {
        BudgetDTO dto = new BudgetDTO();
        dto.setId(budget.getId());
        dto.setPeriodId(budget.getPeriod().getId());
        dto.setPeriodName(budget.getPeriod().getName());

        if (budget.getCategory() != null) {
            dto.setCategoryId(budget.getCategory().getId());
            dto.setCategoryName(budget.getCategory().getName());
        } else {
            dto.setCategoryName(GENERAL_CATEGORY_NAME);
        }

        dto.setPlannedIncome(budget.getPlannedIncome());
        dto.setPlannedExpenses(budget.getPlannedExpenses());
        dto.setActualIncome(budget.getActualIncome());
        dto.setActualExpenses(budget.getActualExpenses());
        dto.setNotes(budget.getNotes());

        return dto;
    }

    /**
     * Determina la categoria dal DTO, restituendo null per categorie generali.
     *
     * @param dto il DTO contenente i dati della categoria
     * @return la categoria trovata/creata o null per budget generali
     */
    private Category determineCategoryFromDTO(BudgetDTO dto) {
        return dto.getCategoryName() != null && !dto.getCategoryName().equals(GENERAL_CATEGORY_NAME)
                ? findOrCreateCategory(dto.getCategoryName())
                : null;
    }

    /**
     * Calcola il totale delle entrate da una lista di movimenti.
     *
     * @param movements lista dei movimenti da analizzare
     * @return totale delle entrate
     */
    private BigDecimal calculateTotalIncome(List<Movement> movements) {
        return movements.stream()
                .filter(m -> m.getType() == MovementType.INCOME)
                .map(Movement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcola le spese filtrate per categoria specifica o totali per budget generali.
     *
     * @param movements lista dei movimenti da analizzare
     * @param category categoria per il filtro (null per budget generali)
     * @return totale delle spese filtrate
     */
    private BigDecimal calculateExpensesForCategory(List<Movement> movements, Category category) {
        if (category != null) {
            // Budget specifico per categoria - solo spese di quella categoria
            return movements.stream()
                    .filter(m -> m.getType() == MovementType.EXPENSE)
                    .filter(m -> m.getCategories().contains(category))
                    .map(Movement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            // Budget generale - tutte le spese del periodo
            return movements.stream()
                    .filter(m -> m.getType() == MovementType.EXPENSE)
                    .map(Movement::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Logga informazioni sui dettagli del calcolo per debug.
     */
    private void logCalculationDetails(List<Movement> movements, Category category,
                                       BigDecimal actualIncome, BigDecimal actualExpenses) {
        System.out.println("BUDGET - Entrate TOTALI periodo (vanno in tutti i budget): €" + actualIncome);

        System.out.println("BUDGET - Movimenti INCOME nel periodo:");
        movements.stream()
                .filter(m -> m.getType() == MovementType.INCOME)
                .forEach(m -> System.out.println("  - " + m.getDescription() + ": €" + m.getAmount()));

        if (category != null) {
            System.out.println("BUDGET - Spese filtrate per categoria " + category.getName() + ": €" + actualExpenses);
            System.out.println("BUDGET - Movimenti EXPENSE per categoria " + category.getName() + ":");
            movements.stream()
                    .filter(m -> m.getType() == MovementType.EXPENSE)
                    .filter(m -> m.getCategories().contains(category))
                    .forEach(m -> System.out.println("  - " + m.getDescription() + ": €" + m.getAmount()));
        } else {
            System.out.println("BUDGET - Spese totali per budget generale: €" + actualExpenses);
            System.out.println("BUDGET - Movimenti EXPENSE totali:");
            movements.stream()
                    .filter(m -> m.getType() == MovementType.EXPENSE)
                    .forEach(m -> System.out.println("  - " + m.getDescription() + ": €" + m.getAmount()));
        }
    }

    /**
     * Logga informazioni di base sul budget in elaborazione.
     */
    private void logBudgetProcessingInfo(Period period, Category category) {
        System.out.println("BUDGET - Processing: " +
                (category != null ? category.getName() : GENERAL_CATEGORY_NAME) +
                " per " + period.getName());
    }

    /**
     * Aggiorna il budget con i nuovi valori calcolati e lo salva nel database.
     */
    private void updateAndSaveBudget(Budget budget, BigDecimal actualIncome,
                                     BigDecimal actualExpenses, Long budgetId) {
        System.out.println("BUDGET - PRIMA dell'aggiornamento entity:");
        System.out.println("  - Budget actualIncome: €" + budget.getActualIncome());
        System.out.println("  - Budget actualExpenses: €" + budget.getActualExpenses());

        budget.updateActuals(actualIncome, actualExpenses);

        System.out.println("BUDGET - DOPO updateActuals ma PRIMA del save:");
        System.out.println("  - Budget actualIncome: €" + budget.getActualIncome());
        System.out.println("  - Budget actualExpenses: €" + budget.getActualExpenses());

        Budget savedBudget = budgetRepository.save(budget);
        logPostSaveVerification(savedBudget, budgetId);
    }

    /**
     * Esegue verifiche dopo il salvataggio per confermare la persistenza dei dati.
     */
    private void logPostSaveVerification(Budget savedBudget, Long budgetId) {
        System.out.println("BUDGET - DOPO il salvataggio:");
        System.out.println("  - SavedBudget actualIncome: €" + savedBudget.getActualIncome());
        System.out.println("  - SavedBudget actualExpenses: €" + savedBudget.getActualExpenses());

        // Verifica ricaricando dal database
        Budget reloadedBudget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget non trovato dopo salvataggio"));

        System.out.println("BUDGET - AFTER RELOAD FROM DB:");
        System.out.println("  - Reloaded actualIncome: €" + reloadedBudget.getActualIncome());
        System.out.println("  - Reloaded actualExpenses: €" + reloadedBudget.getActualExpenses());

        // Test conversione DTO
        BudgetDTO testDTO = convertToDTO(reloadedBudget);
        System.out.println("BUDGET - DTO CONVERSION TEST:");
        System.out.println("  - DTO actualIncome: €" + testDTO.getActualIncome());
        System.out.println("  - DTO actualExpenses: €" + testDTO.getActualExpenses());

        System.out.println("BUDGET - Aggiornamento completato per budget ID: " + budgetId);
    }

    /**
     * Trova un periodo esistente per nome o ne crea uno nuovo.
     *
     * @param periodName nome del periodo da trovare/creare
     * @return il periodo trovato o creato
     */
    private Period findOrCreatePeriod(String periodName) {
        System.out.println("PERIOD - Ricerca/creazione periodo: " + periodName);

        try {
            // Cerca nei periodi esistenti per nome
            Optional<Period> existingPeriod = findExistingPeriodByName(periodName);
            if (existingPeriod.isPresent()) {
                return existingPeriod.get();
            }

            // Se non esiste, crea nuovo periodo
            return createNewPeriod(periodName);

        } catch (Exception e) {
            System.err.println("PERIOD - Errore durante ricerca/creazione periodo: " + e.getMessage());
            return createFallbackPeriod();
        }
    }

    /**
     * Cerca un periodo esistente per nome.
     */
    private Optional<Period> findExistingPeriodByName(String periodName) {
        List<Period> allPeriods = periodRepository.findAll();
        return allPeriods.stream()
                .filter(period -> periodName.equals(period.getName()))
                .findFirst()
                .map(period -> {
                    System.out.println("PERIOD - Periodo esistente trovato: " + period.getName() +
                            " (" + period.getStartDate() + " - " + period.getEndDate() + ")");
                    return period;
                });
    }

    /**
     * Crea un nuovo periodo basandosi sul nome fornito.
     */
    private Period createNewPeriod(String periodName) {
        System.out.println("PERIOD - Periodo non esistente, creazione in corso...");

        PeriodDateRange dateRange = calculateDateRangeForPeriod(periodName);

        System.out.println("PERIOD - Creazione nuovo periodo: " + periodName +
                " dal " + dateRange.startDate + " al " + dateRange.endDate);

        Period newPeriod = new Period(periodName, dateRange.startDate, dateRange.endDate);
        Period savedPeriod = periodRepository.save(newPeriod);

        System.out.println("PERIOD - Periodo creato con successo: ID=" + savedPeriod.getId() +
                ", Nome=" + savedPeriod.getName());

        return savedPeriod;
    }

    /**
     * Crea un periodo di fallback per il mese corrente.
     */
    private Period createFallbackPeriod() {
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());

        System.out.println("PERIOD - Fallback: creazione periodo mese corrente");
        Period fallbackPeriod = new Period(CURRENT_MONTH_FALLBACK, startDate, endDate);
        return periodRepository.save(fallbackPeriod);
    }

    /**
     * Calcola l'intervallo di date per un periodo basandosi sul nome.
     */
    private PeriodDateRange calculateDateRangeForPeriod(String periodName) {
        // Mappa dei periodi supportati
        Map<String, PeriodDateRange> periodMap = createPeriodDateMap();

        PeriodDateRange range = periodMap.get(periodName);
        if (range != null) {
            return range;
        }

        // Fallback per periodi sconosciuti
        System.out.println("PERIOD - Periodo sconosciuto: " + periodName + ", usando mese corrente");
        LocalDate now = LocalDate.now();
        return new PeriodDateRange(
                now.withDayOfMonth(1),
                now.withDayOfMonth(now.lengthOfMonth())
        );
    }

    /**
     * Crea la mappa dei periodi supportati con le relative date.
     * Utilizza un approccio più mantenibile rispetto a un long switch.
     */
    /**
     * Crea la mappa dei periodi supportati con le relative date.
     * Utilizza un approccio più mantenibile rispetto a un long switch.
     */
    private Map<String, PeriodDateRange> createPeriodDateMap() {
        Map<String, PeriodDateRange> periodMap = new HashMap<>();

        // Anno 2025
        periodMap.put("Gennaio 2025", new PeriodDateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));
        periodMap.put("Febbraio 2025", new PeriodDateRange(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28)));
        periodMap.put("Marzo 2025", new PeriodDateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)));
        periodMap.put("Aprile 2025", new PeriodDateRange(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30)));
        periodMap.put("Maggio 2025", new PeriodDateRange(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 31)));
        periodMap.put("Giugno 2025", new PeriodDateRange(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30)));
        periodMap.put("Luglio 2025", new PeriodDateRange(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31)));
        periodMap.put("Agosto 2025", new PeriodDateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 31)));
        periodMap.put("Settembre 2025", new PeriodDateRange(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30)));
        periodMap.put("Ottobre 2025", new PeriodDateRange(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 31)));
        periodMap.put("Novembre 2025", new PeriodDateRange(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 30)));
        periodMap.put("Dicembre 2025", new PeriodDateRange(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)));

        // Anno 2026
        periodMap.put("Gennaio 2026", new PeriodDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
        periodMap.put("Febbraio 2026", new PeriodDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)));
        periodMap.put("Marzo 2026", new PeriodDateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));
        periodMap.put("Aprile 2026", new PeriodDateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)));
        periodMap.put("Maggio 2026", new PeriodDateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        periodMap.put("Giugno 2026", new PeriodDateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));
        periodMap.put("Luglio 2026", new PeriodDateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)));
        periodMap.put("Agosto 2026", new PeriodDateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));
        periodMap.put("Settembre 2026", new PeriodDateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
        periodMap.put("Ottobre 2026", new PeriodDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        periodMap.put("Novembre 2026", new PeriodDateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)));
        periodMap.put("Dicembre 2026", new PeriodDateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)));

        return periodMap;
    }

    /**
     * Trova una categoria esistente per nome o ne crea una nuova.
     *
     * @param categoryName nome della categoria da trovare/creare
     * @return la categoria trovata o creata
     */
    private Category findOrCreateCategory(String categoryName) {
        List<Category> existingCategories = categoryRepository.findByName(categoryName);
        if (!existingCategories.isEmpty()) {
            return existingCategories.get(0);
        }

        Category category = new Category(categoryName);
        return categoryRepository.save(category);
    }

    /**
     * Classe di supporto per rappresentare un intervallo di date per un periodo.
     */
    private static class PeriodDateRange {
        final LocalDate startDate;
        final LocalDate endDate;

        PeriodDateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}