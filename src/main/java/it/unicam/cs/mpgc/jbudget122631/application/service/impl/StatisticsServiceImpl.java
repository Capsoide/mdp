package it.unicam.cs.mpgc.jbudget122631.application.service.impl;

import it.unicam.cs.mpgc.jbudget122631.application.dto.StatisticsDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.StatisticsService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.*;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementazione del servizio per il calcolo e l'analisi delle statistiche finanziarie.
 *
 * Gestisce la logica di business per:
 * - Calcolo di totali per periodo e categoria
 * - Generazione di trend mensili per entrate e uscite
 * - Analisi comparative tra periodi diversi
 * - Valutazione delle performance dei budget
 * - Classifiche delle categorie di spesa più utilizzate
 *
 * Caratteristiche principali:
 * - Aggregazioni complesse con filtri temporali e per categoria
 * - Analisi di trend con granularità mensile
 * - Confronti percentuali e di varianza per i budget
 * - Supporto per classifiche limitate delle top spending categories
 * - Gestione di periodi con date parziali (inizio/fine periodo)
 *
 * @author Sistema Budget Familiare
 * @version 1.0
 * @since 2024
 */
public class StatisticsServiceImpl implements StatisticsService {

    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Categoria non trovata";
    private static final String PERIOD_NOT_FOUND_MESSAGE = "Periodo non trovato";
    private static final String PERIOD_1_KEY = "period1";
    private static final String PERIOD_2_KEY = "period2";

    // Chiavi per l'analisi delle performance dei budget
    private static final String TOTAL_BUDGETS_KEY = "totalBudgets";
    private static final String OVER_BUDGET_COUNT_KEY = "overBudgetCount";
    private static final String TOTAL_VARIANCE_KEY = "totalVariance";
    private static final String OVER_BUDGET_PERCENTAGE_KEY = "overBudgetPercentage";

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final MovementRepository movementRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Costruttore per l'iniezione delle dipendenze dei repository.
     *
     * @param movementRepository repository per l'accesso ai movimenti
     * @param budgetRepository repository per l'accesso ai budget
     * @param categoryRepository repository per l'accesso alle categorie
     */
    public StatisticsServiceImpl(MovementRepository movementRepository,
                                 BudgetRepository budgetRepository,
                                 CategoryRepository categoryRepository) {
        this.movementRepository = movementRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Calcola statistiche complete per un periodo specificato.
     *
     * Include:
     * - Totali generali di entrate, uscite e bilancio
     * - Suddivisioni per categoria
     * - Trend mensile del periodo
     *
     * @param startDate data di inizio periodo
     * @param endDate data di fine periodo
     * @return DTO con tutte le statistiche calcolate per il periodo
     */
    @Override
    public StatisticsDTO getStatisticsForPeriod(LocalDate startDate, LocalDate endDate) {
        StatisticsDTO stats = new StatisticsDTO(startDate, endDate);

        // Calcola totali generali usando query ottimizzate
        PeriodTotals totals = calculatePeriodTotals(startDate, endDate);
        populateBasicStatistics(stats, totals);

        // Calcola statistiche dettagliate per categoria
        stats.setIncomeByCategory(calculateIncomeByCategory(startDate, endDate));
        stats.setExpensesByCategory(calculateExpensesByCategory(startDate, endDate));

        // Calcola trend mensile per il periodo
        stats.setMonthlyTrend(calculateMonthlyTrend(startDate, endDate));

        return stats;
    }

    /**
     * Calcola statistiche specifiche per una categoria in un periodo.
     *
     * @param categoryId ID della categoria da analizzare
     * @param startDate data di inizio periodo
     * @param endDate data di fine periodo
     * @return DTO con statistiche specifiche della categoria
     * @throws RuntimeException se la categoria non esiste
     */
    @Override
    public StatisticsDTO getStatisticsForCategory(Long categoryId, LocalDate startDate, LocalDate endDate) {
        Category category = findCategoryById(categoryId);
        StatisticsDTO stats = new StatisticsDTO(startDate, endDate);

        CategoryTotals categoryTotals = calculateCategoryTotals(category, startDate, endDate);
        populateCategoryStatistics(stats, categoryTotals);

        return stats;
    }

    /**
     * Confronta le statistiche tra due periodi diversi.
     *
     * @param period1Start data inizio primo periodo
     * @param period1End data fine primo periodo
     * @param period2Start data inizio secondo periodo
     * @param period2End data fine secondo periodo
     * @return mappa contenente le statistiche di entrambi i periodi
     */
    @Override
    public Map<String, StatisticsDTO> comparePeriodsStatistics(LocalDate period1Start, LocalDate period1End,
                                                               LocalDate period2Start, LocalDate period2End) {
        Map<String, StatisticsDTO> comparison = new LinkedHashMap<>();

        comparison.put(PERIOD_1_KEY, getStatisticsForPeriod(period1Start, period1End));
        comparison.put(PERIOD_2_KEY, getStatisticsForPeriod(period2Start, period2End));

        return comparison;
    }

    @Override
    public Map<String, BigDecimal> getMonthlyIncomeExpensesTrend(LocalDate startDate, LocalDate endDate) {
        return calculateMonthlyTrend(startDate, endDate);
    }

    /**
     * Calcola il trend di spesa mensile per una categoria specifica.
     *
     * @param categoryId ID della categoria da analizzare
     * @param startDate data di inizio periodo
     * @param endDate data di fine periodo
     * @return mappa con trend mensile della categoria (formato YYYY-MM -> importo)
     * @throws RuntimeException se la categoria non esiste
     */
    @Override
    public Map<String, BigDecimal> getCategorySpendingTrend(Long categoryId, LocalDate startDate, LocalDate endDate) {
        Category category = findCategoryById(categoryId);
        return calculateCategoryMonthlyTrend(category, startDate, endDate);
    }

    /**
     * Analizza le performance dei budget per un periodo specifico.
     *
     * Calcola metriche come:
     * - Numero totale di budget
     * - Numero di budget sforati
     * - Varianza totale (differenza tra pianificato e reale)
     * - Percentuale di budget sforati
     *
     * @param periodId ID del periodo da analizzare
     * @return mappa con le metriche di performance
     * @throws RuntimeException se il periodo non esiste
     */
    @Override
    public Map<String, Object> getBudgetPerformanceAnalysis(Long periodId) {
        Period period = findPeriodFromBudgets(periodId);
        List<Budget> budgets = budgetRepository.findByPeriod(period);

        return createBudgetAnalysis(budgets);
    }

    /**
     * Identifica le categorie con maggiori spese in un periodo.
     *
     * @param startDate data di inizio periodo
     * @param endDate data di fine periodo
     * @param limit numero massimo di categorie da restituire
     * @return mappa ordinata per importo decrescente delle top spending categories
     */
    @Override
    public Map<String, BigDecimal> getTopSpendingCategories(LocalDate startDate, LocalDate endDate, int limit) {
        List<Category> activeCategories = categoryRepository.findActiveCategories();

        return activeCategories.stream()
                .collect(Collectors.toMap(
                        Category::getName,
                        cat -> calculateCategoryExpenses(cat, startDate, endDate)
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // === METODI PRIVATI DI SUPPORTO ===

    /**
     * Trova una categoria per ID o lancia eccezione.
     */
    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException(CATEGORY_NOT_FOUND_MESSAGE + " con ID: " + categoryId));
    }

    /**
     * Trova un periodo dal repository budget (metodo di supporto).
     */
    private Period findPeriodFromBudgets(Long periodId) {
        return budgetRepository.findById(periodId)
                .map(Budget::getPeriod)
                .orElseThrow(() -> new RuntimeException(PERIOD_NOT_FOUND_MESSAGE + " con ID: " + periodId));
    }

    // === CALCOLI TOTALI ===

    /**
     * Calcola i totali generali per un periodo usando query ottimizzate.
     */
    private PeriodTotals calculatePeriodTotals(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalIncome = movementRepository.getTotalByTypeAndDateRange(
                MovementType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = movementRepository.getTotalByTypeAndDateRange(
                MovementType.EXPENSE, startDate, endDate);

        return new PeriodTotals(totalIncome, totalExpenses);
    }

    /**
     * Calcola i totali per una categoria specifica in un periodo.
     */
    private CategoryTotals calculateCategoryTotals(Category category, LocalDate startDate, LocalDate endDate) {
        List<Movement> movements = getMovementsInDateRange(category, startDate, endDate);

        BigDecimal income = sumMovementsByType(movements, true);
        BigDecimal expenses = sumMovementsByType(movements, false);

        return new CategoryTotals(income, expenses);
    }

    /**
     * Recupera i movimenti di una categoria filtrati per intervallo di date.
     */
    private List<Movement> getMovementsInDateRange(Category category, LocalDate startDate, LocalDate endDate) {
        return movementRepository.findByCategory(category)
                .stream()
                .filter(m -> isMovementInDateRange(m, startDate, endDate))
                .collect(Collectors.toList());
    }

    /**
     * Verifica se un movimento rientra nell'intervallo di date specificato.
     */
    private boolean isMovementInDateRange(Movement movement, LocalDate startDate, LocalDate endDate) {
        LocalDate movementDate = movement.getDate();
        return !movementDate.isBefore(startDate) && !movementDate.isAfter(endDate);
    }

    /**
     * Somma i movimenti filtrando per tipo (entrate o uscite).
     */
    private BigDecimal sumMovementsByType(List<Movement> movements, boolean isIncome) {
        return movements.stream()
                .filter(m -> isIncome ? m.isIncome() : m.isExpense())
                .map(Movement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // === POPOLAMENTO STATISTICHE ===

    /**
     * Popola le statistiche di base dal calcolo dei totali.
     */
    private void populateBasicStatistics(StatisticsDTO stats, PeriodTotals totals) {
        stats.setTotalIncome(totals.income);
        stats.setTotalExpenses(totals.expenses);
        stats.setBalance(totals.income.subtract(totals.expenses));
    }

    /**
     * Popola le statistiche specifiche di una categoria.
     */
    private void populateCategoryStatistics(StatisticsDTO stats, CategoryTotals totals) {
        stats.setTotalIncome(totals.income);
        stats.setTotalExpenses(totals.expenses);
        stats.setBalance(totals.income.subtract(totals.expenses));
    }

    // === CALCOLI PER CATEGORIA ===

    /**
     * Calcola le entrate suddivise per categoria in un periodo.
     */
    private Map<String, BigDecimal> calculateIncomeByCategory(LocalDate startDate, LocalDate endDate) {
        return calculateAmountsByCategory(startDate, endDate, true);
    }

    /**
     * Calcola le uscite suddivise per categoria in un periodo.
     */
    private Map<String, BigDecimal> calculateExpensesByCategory(LocalDate startDate, LocalDate endDate) {
        return calculateAmountsByCategory(startDate, endDate, false);
    }

    /**
     * Calcola gli importi suddivisi per categoria, filtrando per tipo movimento.
     */
    private Map<String, BigDecimal> calculateAmountsByCategory(LocalDate startDate, LocalDate endDate, boolean isIncome) {
        List<Category> activeCategories = categoryRepository.findActiveCategories();
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (Category category : activeCategories) {
            BigDecimal total = calculateCategoryAmountByType(category, startDate, endDate, isIncome);

            if (total.compareTo(BigDecimal.ZERO) > 0) {
                result.put(category.getName(), total);
            }
        }

        return result;
    }

    /**
     * Calcola l'importo di una categoria per un tipo specifico di movimento.
     */
    private BigDecimal calculateCategoryAmountByType(Category category, LocalDate startDate, LocalDate endDate, boolean isIncome) {
        List<Movement> movements = getMovementsInDateRange(category, startDate, endDate);
        return sumMovementsByType(movements, isIncome);
    }

    /**
     * Calcola solo le uscite per una categoria (metodo di supporto).
     */
    private BigDecimal calculateCategoryExpenses(Category category, LocalDate startDate, LocalDate endDate) {
        return calculateCategoryAmountByType(category, startDate, endDate, false);
    }

    // === CALCOLI TREND ===

    /**
     * Calcola il trend mensile generale (bilancio) per un periodo.
     */
    private Map<String, BigDecimal> calculateMonthlyTrend(LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> monthlyTrend = new LinkedHashMap<>();

        processMonthsInRange(startDate, endDate, (monthStart, monthEnd, yearMonth) -> {
            BigDecimal monthlyBalance = calculateMonthlyBalance(monthStart, monthEnd);
            monthlyTrend.put(yearMonth.toString(), monthlyBalance);
        });

        return monthlyTrend;
    }

    /**
     * Calcola il trend mensile per una categoria specifica.
     */
    private Map<String, BigDecimal> calculateCategoryMonthlyTrend(Category category, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> trend = new LinkedHashMap<>();

        processMonthsInRange(startDate, endDate, (monthStart, monthEnd, yearMonth) -> {
            BigDecimal monthlyTotal = movementRepository.getTotalByCategoryAndDateRange(category, monthStart, monthEnd);
            trend.put(yearMonth.toString(), monthlyTotal);
        });

        return trend;
    }

    /**
     * Processa tutti i mesi in un intervallo di date con una funzione callback.
     */
    private void processMonthsInRange(LocalDate startDate, LocalDate endDate, MonthProcessor processor) {
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);

        YearMonth current = start;
        while (!current.isAfter(end)) {
            DateRange monthRange = calculateMonthRange(current, startDate, endDate);
            processor.process(monthRange.start, monthRange.end, current);
            current = current.plusMonths(1);
        }
    }

    /**
     * Calcola l'intervallo di date effettivo per un mese, considerando i limiti del periodo.
     */
    private DateRange calculateMonthRange(YearMonth yearMonth, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // Adatta alle date del periodo se necessario
        if (monthStart.isBefore(periodStart)) monthStart = periodStart;
        if (monthEnd.isAfter(periodEnd)) monthEnd = periodEnd;

        return new DateRange(monthStart, monthEnd);
    }

    /**
     * Calcola il bilancio mensile (entrate - uscite) per un mese specifico.
     */
    private BigDecimal calculateMonthlyBalance(LocalDate monthStart, LocalDate monthEnd) {
        BigDecimal monthlyIncome = movementRepository.getTotalByTypeAndDateRange(
                MovementType.INCOME, monthStart, monthEnd);
        BigDecimal monthlyExpenses = movementRepository.getTotalByTypeAndDateRange(
                MovementType.EXPENSE, monthStart, monthEnd);

        return monthlyIncome.subtract(monthlyExpenses);
    }

    // === ANALISI BUDGET ===

    /**
     * Crea l'analisi completa delle performance dei budget.
     */
    private Map<String, Object> createBudgetAnalysis(List<Budget> budgets) {
        Map<String, Object> analysis = new LinkedHashMap<>();

        BudgetMetrics metrics = calculateBudgetMetrics(budgets);

        analysis.put(TOTAL_BUDGETS_KEY, budgets.size());
        analysis.put(OVER_BUDGET_COUNT_KEY, metrics.overBudgetCount);
        analysis.put(TOTAL_VARIANCE_KEY, metrics.totalVariance);
        analysis.put(OVER_BUDGET_PERCENTAGE_KEY, metrics.overBudgetPercentage);

        return analysis;
    }

    /**
     * Calcola le metriche aggregate per una lista di budget.
     */
    private BudgetMetrics calculateBudgetMetrics(List<Budget> budgets) {
        if (budgets.isEmpty()) {
            return new BudgetMetrics(0L, BigDecimal.ZERO, 0.0);
        }

        long overBudgetCount = budgets.stream()
                .mapToLong(b -> b.isOverBudget() ? 1 : 0)
                .sum();

        BigDecimal totalVariance = budgets.stream()
                .map(Budget::getVarianceBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double overBudgetPercentage = calculatePercentage(overBudgetCount, budgets.size());

        return new BudgetMetrics(overBudgetCount, totalVariance, overBudgetPercentage);
    }

    /**
     * Calcola una percentuale con gestione della divisione per zero.
     */
    private double calculatePercentage(long numerator, int denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // === INTERFACCE E CLASSI DI SUPPORTO ===

    /**
     * Interface funzionale per processare mesi in un intervallo.
     */
    @FunctionalInterface
    private interface MonthProcessor {
        void process(LocalDate monthStart, LocalDate monthEnd, YearMonth yearMonth);
    }

    /**
     * Rappresenta un intervallo di date.
     */
    private static class DateRange {
        final LocalDate start;
        final LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Contiene i totali calcolati per un periodo generale.
     */
    private static class PeriodTotals {
        final BigDecimal income;
        final BigDecimal expenses;

        PeriodTotals(BigDecimal income, BigDecimal expenses) {
            this.income = income != null ? income : BigDecimal.ZERO;
            this.expenses = expenses != null ? expenses : BigDecimal.ZERO;
        }
    }

    /**
     * Contiene i totali calcolati per una categoria specifica.
     */
    private static class CategoryTotals {
        final BigDecimal income;
        final BigDecimal expenses;

        CategoryTotals(BigDecimal income, BigDecimal expenses) {
            this.income = income;
            this.expenses = expenses;
        }
    }

    /**
     * Contiene le metriche aggregate per l'analisi dei budget.
     */
    private static class BudgetMetrics {
        final long overBudgetCount;
        final BigDecimal totalVariance;
        final double overBudgetPercentage;

        BudgetMetrics(long overBudgetCount, BigDecimal totalVariance, double overBudgetPercentage) {
            this.overBudgetCount = overBudgetCount;
            this.totalVariance = totalVariance;
            this.overBudgetPercentage = overBudgetPercentage;
        }
    }
}