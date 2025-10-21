package it.unicam.cs.mpgc.jbudget122631.domain.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "movements")
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "movement_categories",
            joinColumns = @JoinColumn(name = "movement_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    private String notes;

    @Column(name = "is_scheduled")
    private boolean scheduled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amortization_plan_id")
    private AmortizationPlan amortizationPlan;

    protected Movement() {}

    public Movement(String description, BigDecimal amount, MovementType type, LocalDate date) {
        this.description = Objects.requireNonNull(description, "Descrizione richiesta");
        this.amount = validateAmount(amount);
        this.type = Objects.requireNonNull(type, "Tipo movimento richiesto");
        this.date = Objects.requireNonNull(date, "Data richiesta");
        this.createdAt = LocalDateTime.now();
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount deve essere >= 0");
        }
        return amount;
    }

    public void addCategory(Category category) {
        Objects.requireNonNull(category, "Categoria non puo' essere null");
        categories.add(category);
        System.out.println("ENTITY - Categoria aggiunta: " + category.getName() + " (Total: " + categories.size() + ")");
    }

    public void removeCategory(Category category) {
        categories.remove(category);
    }

    public boolean hasCategory(Category category) {
        return categories.contains(category);
    }

    public boolean isIncome() {
        return type == MovementType.INCOME;
    }

    public boolean isExpense() {
        return type == MovementType.EXPENSE;
    }


    public BigDecimal getSignedAmount() {
        return isIncome() ? amount : amount.negate();
    }

    public void updateDetails(String description, BigDecimal amount, MovementType type, LocalDate date, String notes) {
        this.description = Objects.requireNonNull(description);
        this.amount = validateAmount(amount);
        this.type = Objects.requireNonNull(type);
        this.date = Objects.requireNonNull(date);
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public MovementType getType() { return type; }
    public LocalDate getDate() { return date; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Set<Category> getCategories() {
        System.out.println("ENTITY - getCategories() chiamato, size: " + categories.size());
        return categories;
    }

    public String getNotes() { return notes; }
    public boolean isScheduled() { return scheduled; }
    public AmortizationPlan getAmortizationPlan() { return amortizationPlan; }

    public void setNotes(String notes) { this.notes = notes; }
    public void setScheduled(boolean scheduled) { this.scheduled = scheduled; }
    public void setAmortizationPlan(AmortizationPlan plan) { this.amortizationPlan = plan; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movement)) return false;
        Movement movement = (Movement) o;
        return Objects.equals(description, movement.description) &&
                Objects.equals(amount, movement.amount) &&
                type == movement.type &&
                Objects.equals(date, movement.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, amount, type, date);
    }

    @Override
    public String toString() {
        return String.format("%s: %s %s (%s) [%d categories]",
                date, type.getDescription(), amount, description, categories.size());
    }
}