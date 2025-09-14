package it.unicam.cs.mpgc.jbudget122631.application.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class BudgetDTO {
    private Long id;
    private Long periodId;
    private String periodName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal plannedIncome;
    private BigDecimal plannedExpenses;
    private BigDecimal actualIncome;
    private BigDecimal actualExpenses;
    private String notes;

    /**
     * Costruttore di default che inizializza tutti i valori BigDecimal a zero
     * per evitare problemi con valori null nei calcoli successivi
     */
    public BudgetDTO() {
        this.plannedIncome = BigDecimal.ZERO;
        this.plannedExpenses = BigDecimal.ZERO;
        this.actualIncome = BigDecimal.ZERO;
        this.actualExpenses = BigDecimal.ZERO;
    }

    /**
     * Metodo helper per gestire valori BigDecimal null restituendo zero come default
     * Elimina la duplicazione di controlli null in tutta la classe
     * @param value il valore BigDecimal da verificare
     * @return il valore originale se non null, altrimenti BigDecimal.ZERO
     */
    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Calcola la percentuale tra un valore attuale e uno pianificato
     * Gestisce automaticamente la divisione per zero restituendo 0%
     * @param actual il valore effettivo
     * @param planned il valore pianificato
     * @return la percentuale con precisione a 4 decimali
     */
    private double calculatePercentage(BigDecimal actual, BigDecimal planned) {
        if (planned.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return actual.divide(planned, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * Restituisce l'identificativo univoco del budget
     * @return l'ID del budget
     */
    public Long getId() { return id; }

    /**
     * Imposta l'identificativo univoco del budget
     * @param id l'ID da assegnare al budget
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Restituisce l'identificativo del periodo di riferimento
     * @return l'ID del periodo associato al budget
     */
    public Long getPeriodId() { return periodId; }

    /**
     * Imposta l'identificativo del periodo di riferimento
     * @param periodId l'ID del periodo da associare al budget
     */
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    /**
     * Restituisce il nome descrittivo del periodo
     * @return la descrizione del periodo di budget
     */
    public String getPeriodName() { return periodName; }

    /**
     * Imposta il nome descrittivo del periodo
     * @param periodName la descrizione del periodo da assegnare
     */
    public void setPeriodName(String periodName) { this.periodName = periodName; }

    /**
     * Restituisce l'identificativo della categoria di budget
     * @return l'ID della categoria associata
     */
    public Long getCategoryId() { return categoryId; }

    /**
     * Imposta l'identificativo della categoria di budget
     * @param categoryId l'ID della categoria da associare
     */
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    /**
     * Restituisce il nome descrittivo della categoria
     * @return la descrizione della categoria di budget
     */
    public String getCategoryName() { return categoryName; }

    /**
     * Imposta il nome descrittivo della categoria
     * @param categoryName la descrizione della categoria da assegnare
     */
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    /**
     * Restituisce le note aggiuntive associate al budget
     * @return le note descrittive del budget
     */
    public String getNotes() { return notes; }

    /**
     * Imposta le note aggiuntive per il budget
     * @param notes le note descrittive da assegnare
     */
    public void setNotes(String notes) { this.notes = notes; }

    /**
     * Restituisce l'importo pianificato per le entrate garantendo un valore non null
     * @return l'importo delle entrate previste o zero se null
     */
    public BigDecimal getPlannedIncome() {
        return nullSafe(plannedIncome);
    }

    /**
     * Imposta l'importo pianificato per le entrate con protezione da valori null
     * @param plannedIncome l'importo delle entrate previste
     */
    public void setPlannedIncome(BigDecimal plannedIncome) {
        this.plannedIncome = nullSafe(plannedIncome);
    }

    /**
     * Restituisce l'importo pianificato per le uscite garantendo un valore non null
     * @return l'importo delle uscite previste o zero se null
     */
    public BigDecimal getPlannedExpenses() {
        return nullSafe(plannedExpenses);
    }

    /**
     * Imposta l'importo pianificato per le uscite con protezione da valori null
     * @param plannedExpenses l'importo delle uscite previste
     */
    public void setPlannedExpenses(BigDecimal plannedExpenses) {
        this.plannedExpenses = nullSafe(plannedExpenses);
    }

    /**
     * Restituisce l'importo effettivo delle entrate garantendo un valore non null
     * @return l'importo reale delle entrate o zero se null
     */
    public BigDecimal getActualIncome() {
        return nullSafe(actualIncome);
    }

    /**
     * Imposta l'importo effettivo delle entrate con protezione da valori null
     * @param actualIncome l'importo reale delle entrate
     */
    public void setActualIncome(BigDecimal actualIncome) {
        this.actualIncome = nullSafe(actualIncome);
    }

    /**
     * Restituisce l'importo effettivo delle uscite garantendo un valore non null
     * @return l'importo reale delle uscite o zero se null
     */
    public BigDecimal getActualExpenses() {
        return nullSafe(actualExpenses);
    }

    /**
     * Imposta l'importo effettivo delle uscite con protezione da valori null
     * @param actualExpenses l'importo reale delle uscite
     */
    public void setActualExpenses(BigDecimal actualExpenses) {
        this.actualExpenses = nullSafe(actualExpenses);
    }

    /**
     * Calcola il saldo pianificato sottraendo le uscite previste dalle entrate previste
     * @return la differenza tra entrate e uscite pianificate
     */
    public BigDecimal getPlannedBalance() {
        return getPlannedIncome().subtract(getPlannedExpenses());
    }

    /**
     * Calcola il saldo effettivo sottraendo le uscite reali dalle entrate reali
     * @return la differenza tra entrate e uscite effettive
     */
    public BigDecimal getActualBalance() {
        return getActualIncome().subtract(getActualExpenses());
    }

    /**
     * Calcola la varianza tra il saldo effettivo e quello pianificato
     * Un valore positivo indica un risultato migliore del previsto
     * @return la differenza tra saldo effettivo e pianificato
     */
    public BigDecimal getVarianceBalance() {
        return getActualBalance().subtract(getPlannedBalance());
    }

    /**
     * Calcola la percentuale di realizzazione delle entrate rispetto al pianificato
     * @return la percentuale di entrate effettive su quelle pianificate
     */
    public double getIncomePercentage() {
        return calculatePercentage(getActualIncome(), getPlannedIncome());
    }

    /**
     * Calcola la percentuale di utilizzo del budget per le uscite
     * @return la percentuale di uscite effettive su quelle pianificate
     */
    public double getExpensesPercentage() {
        return calculatePercentage(getActualExpenses(), getPlannedExpenses());
    }

    /**
     * Determina se il budget e stato sforato confrontando uscite effettive e pianificate
     * @return true se le uscite effettive superano quelle pianificate
     */
    public boolean isOverBudget() {
        return getActualExpenses().compareTo(getPlannedExpenses()) > 0;
    }

    /**
     * Confronta due oggetti BudgetDTO per uguaglianza basandosi sull'ID
     * @param o l'oggetto da confrontare
     * @return true se gli oggetti hanno lo stesso ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BudgetDTO)) return false;
        BudgetDTO budgetDTO = (BudgetDTO) o;
        return Objects.equals(id, budgetDTO.id);
    }

    /**
     * Genera il codice hash basato sull'ID univoco dell'oggetto
     * @return il codice hash calcolato sull'ID
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}