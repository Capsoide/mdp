package it.unicam.cs.mpgc.jbudget122631.domain.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "budgets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"period_id", "category_id"})
})
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private Period period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // null = budget generale

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal plannedIncome = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal plannedExpenses = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal actualIncome = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal actualExpenses = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    // Costruttori
    protected Budget() {} // JPA

    public Budget(Period period, Category category) {
        this.period = Objects.requireNonNull(period, "Periodo richiesto");
        this.category = category; // può essere null per budget generale
        this.createdAt = LocalDateTime.now();
    }

    public Budget(Period period, Category category, BigDecimal plannedIncome, BigDecimal plannedExpenses) {
        this(period, category);
        setPlannedIncome(plannedIncome);
        setPlannedExpenses(plannedExpenses);
    }

    // Metodi business
    public BigDecimal getPlannedBalance() {
        return plannedIncome.subtract(plannedExpenses);
    }

    public BigDecimal getActualBalance() {
        return actualIncome.subtract(actualExpenses);
    }

    public BigDecimal getVarianceIncome() {
        return actualIncome.subtract(plannedIncome);
    }

    public BigDecimal getVarianceExpenses() {
        return actualExpenses.subtract(plannedExpenses);
    }

    public BigDecimal getVarianceBalance() {
        return getActualBalance().subtract(getPlannedBalance());
    }

    public double getIncomePercentage() {
        if (plannedIncome.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return actualIncome.divide(plannedIncome, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public double getExpensesPercentage() {
        if (plannedExpenses.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return actualExpenses.divide(plannedExpenses, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    public boolean isOverBudget() {
        return actualExpenses.compareTo(plannedExpenses) > 0;
    }

    public boolean isGeneral() {
        return category == null;
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Importo deve essere >= 0");
        }
        return amount;
    }

    // METODO MODIFICATO CON DEBUG
    public void updateActuals(BigDecimal actualIncome, BigDecimal actualExpenses) {
        System.out.println("ENTITY - updateActuals chiamato per Budget ID: " + this.id);
        System.out.println("ENTITY - Input actualIncome: €" + actualIncome);
        System.out.println("ENTITY - Input actualExpenses: €" + actualExpenses);
        System.out.println("ENTITY - Valori attuali PRIMA dell'aggiornamento:");
        System.out.println("  - this.actualIncome: €" + this.actualIncome);
        System.out.println("  - this.actualExpenses: €" + this.actualExpenses);

        this.actualIncome = validateAmount(actualIncome);
        this.actualExpenses = validateAmount(actualExpenses);
        this.updatedAt = LocalDateTime.now();

        System.out.println("ENTITY - Valori DOPO l'aggiornamento:");
        System.out.println("  - this.actualIncome: €" + this.actualIncome);
        System.out.println("  - this.actualExpenses: €" + this.actualExpenses);
        System.out.println("  - updatedAt: " + this.updatedAt);
    }

    // Getters/Setters
    public Long getId() { return id; }
    public Period getPeriod() { return period; }
    public Category getCategory() { return category; }
    public BigDecimal getPlannedIncome() { return plannedIncome; }
    public BigDecimal getPlannedExpenses() { return plannedExpenses; }
    public BigDecimal getActualIncome() { return actualIncome; }
    public BigDecimal getActualExpenses() { return actualExpenses; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getNotes() { return notes; }
    public boolean isActive() { return active; }

    public void setPlannedIncome(BigDecimal plannedIncome) {
        this.plannedIncome = validateAmount(plannedIncome);
        this.updatedAt = LocalDateTime.now();
    }

    public void setPlannedExpenses(BigDecimal plannedExpenses) {
        this.plannedExpenses = validateAmount(plannedExpenses);
        this.updatedAt = LocalDateTime.now();
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Budget)) return false;
        Budget budget = (Budget) o;
        return Objects.equals(period, budget.period) &&
                Objects.equals(category, budget.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, category);
    }

    @Override
    public String toString() {
        String categoryName = isGeneral() ? "Generale" : category.getName();
        return String.format("Budget %s - %s: Piano(€%.2f - €%.2f) Reale(€%.2f - €%.2f)",
                categoryName, period.getName(), plannedIncome, plannedExpenses,
                actualIncome, actualExpenses);
    }
}