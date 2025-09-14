package it.unicam.cs.mpgc.jbudget122631.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class StatisticsDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal balance;
    private Map<String, BigDecimal> incomeByCategory;
    private Map<String, BigDecimal> expensesByCategory;
    private Map<String, BigDecimal> monthlyTrend;

    /**
     * Costruttore di default che inizializza tutte le mappe e i valori BigDecimal
     * per evitare null pointer exceptions
     */
    public StatisticsDTO() {
        this.totalIncome = BigDecimal.ZERO;
        this.totalExpenses = BigDecimal.ZERO;
        this.balance = BigDecimal.ZERO;
        this.incomeByCategory = new HashMap<>();
        this.expensesByCategory = new HashMap<>();
        this.monthlyTrend = new HashMap<>();
    }

    /**
     * Costruttore parametrizzato per creare statistiche con periodo specifico
     * Inizializza automaticamente tutte le collezioni e i valori numerici
     * @param startDate la data di inizio del periodo di analisi
     * @param endDate la data di fine del periodo di analisi
     */
    public StatisticsDTO(LocalDate startDate, LocalDate endDate) {
        this();
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Metodo helper per gestire valori BigDecimal null restituendo zero come default
     * @param value il valore BigDecimal da verificare
     * @return il valore originale se non null, altrimenti BigDecimal.ZERO
     */
    private BigDecimal nullSafeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Metodo helper per gestire mappe null restituendo una mappa vuota
     * @param map la mappa da verificare
     * @return la mappa originale se non null, altrimenti una mappa vuota
     */
    private Map<String, BigDecimal> nullSafeMap(Map<String, BigDecimal> map) {
        return map != null ? map : new HashMap<>();
    }

    /**
     * Restituisce la data di inizio del periodo di analisi
     * @return la data di inizio delle statistiche
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Imposta la data di inizio del periodo di analisi
     * @param startDate la data di inizio da assegnare
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Restituisce la data di fine del periodo di analisi
     * @return la data di fine delle statistiche
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Imposta la data di fine del periodo di analisi
     * @param endDate la data di fine da assegnare
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Restituisce il totale delle entrate garantendo un valore non null
     * @return l'importo totale delle entrate o zero se null
     */
    public BigDecimal getTotalIncome() {
        return nullSafeBigDecimal(totalIncome);
    }

    /**
     * Imposta il totale delle entrate con protezione da valori null
     * @param totalIncome l'importo totale delle entrate da assegnare
     */
    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = nullSafeBigDecimal(totalIncome);
    }

    /**
     * Restituisce il totale delle uscite garantendo un valore non null
     * @return l'importo totale delle uscite o zero se null
     */
    public BigDecimal getTotalExpenses() {
        return nullSafeBigDecimal(totalExpenses);
    }

    /**
     * Imposta il totale delle uscite con protezione da valori null
     * @param totalExpenses l'importo totale delle uscite da assegnare
     */
    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = nullSafeBigDecimal(totalExpenses);
    }

    /**
     * Restituisce il saldo calcolato garantendo un valore non null
     * @return il saldo o zero se null
     */
    public BigDecimal getBalance() {
        return nullSafeBigDecimal(balance);
    }

    /**
     * Imposta il saldo con protezione da valori null
     * @param balance il saldo da assegnare
     */
    public void setBalance(BigDecimal balance) {
        this.balance = nullSafeBigDecimal(balance);
    }

    /**
     * Restituisce la mappa delle entrate per categoria garantendo una mappa non null
     * @return la mappa delle entrate per categoria o una mappa vuota se null
     */
    public Map<String, BigDecimal> getIncomeByCategory() {
        return nullSafeMap(incomeByCategory);
    }

    /**
     * Imposta la mappa delle entrate per categoria con protezione da valori null
     * @param incomeByCategory la mappa delle entrate per categoria da assegnare
     */
    public void setIncomeByCategory(Map<String, BigDecimal> incomeByCategory) {
        this.incomeByCategory = nullSafeMap(incomeByCategory);
    }

    /**
     * Restituisce la mappa delle uscite per categoria garantendo una mappa non null
     * @return la mappa delle uscite per categoria o una mappa vuota se null
     */
    public Map<String, BigDecimal> getExpensesByCategory() {
        return nullSafeMap(expensesByCategory);
    }

    /**
     * Imposta la mappa delle uscite per categoria con protezione da valori null
     * @param expensesByCategory la mappa delle uscite per categoria da assegnare
     */
    public void setExpensesByCategory(Map<String, BigDecimal> expensesByCategory) {
        this.expensesByCategory = nullSafeMap(expensesByCategory);
    }

    /**
     * Restituisce la mappa del trend mensile garantendo una mappa non null
     * @return la mappa del trend mensile o una mappa vuota se null
     */
    public Map<String, BigDecimal> getMonthlyTrend() {
        return nullSafeMap(monthlyTrend);
    }

    /**
     * Imposta la mappa del trend mensile con protezione da valori null
     * @param monthlyTrend la mappa del trend mensile da assegnare
     */
    public void setMonthlyTrend(Map<String, BigDecimal> monthlyTrend) {
        this.monthlyTrend = nullSafeMap(monthlyTrend);
    }

    /**
     * Calcola automaticamente il saldo sottraendo le uscite dalle entrate
     * Utile per mantenere coerenza nei dati
     */
    public void calculateBalance() {
        this.balance = getTotalIncome().subtract(getTotalExpenses());
    }

    /**
     * Verifica se il periodo di analisi è valido
     * @return true se entrambe le date sono presenti e la data di fine è dopo quella di inizio
     */
    public boolean isValidPeriod() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    /**
     * Verifica se ci sono dati di movimento nel periodo
     * @return true se ci sono entrate o uscite registrate
     */
    public boolean hasMovements() {
        return getTotalIncome().compareTo(BigDecimal.ZERO) > 0 ||
                getTotalExpenses().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Confronta due oggetti StatisticsDTO per uguaglianza basandosi su tutti i campi principali
     * @param o l'oggetto da confrontare
     * @return true se gli oggetti sono equivalenti
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatisticsDTO)) return false;
        StatisticsDTO that = (StatisticsDTO) o;
        return Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                Objects.equals(getTotalIncome(), that.getTotalIncome()) &&
                Objects.equals(getTotalExpenses(), that.getTotalExpenses());
    }

    /**
     * Genera il codice hash basato sui campi principali dell'oggetto
     * @return il codice hash calcolato
     */
    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate, getTotalIncome(), getTotalExpenses());
    }

    /**
     * Restituisce una rappresentazione testuale delle statistiche con protezione da valori null
     * @return una stringa che descrive le statistiche principali
     */
    @Override
    public String toString() {
        return String.format("StatisticsDTO{startDate=%s, endDate=%s, totalIncome=%s, totalExpenses=%s, balance=%s}",
                Objects.toString(startDate, "null"),
                Objects.toString(endDate, "null"),
                Objects.toString(getTotalIncome(), "0"),
                Objects.toString(getTotalExpenses(), "0"),
                Objects.toString(getBalance(), "0"));
    }
}