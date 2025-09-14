package it.unicam.cs.mpgc.jbudget122631.domain.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "scheduled_expenses")
public class ScheduledExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type = MovementType.EXPENSE;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType = RecurrenceType.NONE;

    private Integer recurrenceInterval = 1; // ogni X unità (es. ogni 2 mesi)

    private LocalDate recurrenceEndDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "scheduled_expense_categories",
            joinColumns = @JoinColumn(name = "scheduled_expense_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String notes;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(nullable = false)
    private boolean active = true;

    // Riferimento al movimento creato (se completato)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_movement_id")
    private Movement createdMovement;

    // Costruttori
    protected ScheduledExpense() {} // JPA

    public ScheduledExpense(String description, BigDecimal amount, LocalDate dueDate) {
        this.description = Objects.requireNonNull(description, "Descrizione richiesta");
        this.amount = validateAmount(amount);
        this.dueDate = Objects.requireNonNull(dueDate, "Data scadenza richiesta");
        this.createdAt = LocalDateTime.now();
    }

    public ScheduledExpense(String description, BigDecimal amount, MovementType type, LocalDate dueDate) {
        this(description, amount, dueDate);
        this.type = Objects.requireNonNull(type, "Tipo movimento richiesto");
    }

    // Metodi business
    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Importo deve essere > 0");
        }
        return amount;
    }

    public void addCategory(Category category) {
        Objects.requireNonNull(category, "Categoria non può essere null");
        categories.add(category);
        updateTimestamp();
    }

    public void removeCategory(Category category) {
        if (categories.remove(category)) {
            updateTimestamp();
        }
    }

    public boolean isDue() {
        return !dueDate.isAfter(LocalDate.now());
    }

    public boolean isOverdue() {
        return dueDate.isBefore(LocalDate.now()) && !completed;
    }

    public long getDaysUntilDue() {
        return LocalDate.now().until(dueDate).getDays();
    }

    public boolean isRecurring() {
        return recurrenceType != RecurrenceType.NONE;
    }

    public LocalDate getNextDueDate() {
        if (!isRecurring()) return null;

        LocalDate nextDate = dueDate;
        LocalDate today = LocalDate.now();

        while (!nextDate.isAfter(today)) {
            nextDate = calculateNextOccurrence(nextDate);
            if (recurrenceEndDate != null && nextDate.isAfter(recurrenceEndDate)) {
                return null;
            }
        }

        return nextDate;
    }

    private LocalDate calculateNextOccurrence(LocalDate currentDate) {
        switch (recurrenceType) {
            case DAILY:
                return currentDate.plusDays(recurrenceInterval);
            case WEEKLY:
                return currentDate.plusWeeks(recurrenceInterval);
            case MONTHLY:
                return currentDate.plusMonths(recurrenceInterval);
            case YEARLY:
                return currentDate.plusYears(recurrenceInterval);
            default:
                return currentDate;
        }
    }

    public Movement createMovement() {
        if (completed) {
            throw new IllegalStateException("Spesa già completata");
        }

        Movement movement = new Movement(description, amount, type, LocalDate.now());
        movement.setNotes(notes);
        categories.forEach(movement::addCategory);

        this.createdMovement = movement;
        this.completed = true;
        updateTimestamp();

        return movement;
    }

    public ScheduledExpense createNextOccurrence() {
        if (!isRecurring()) {
            throw new IllegalStateException("Spesa non ricorrente");
        }

        LocalDate nextDate = getNextDueDate();
        if (nextDate == null) {
            throw new IllegalStateException("Nessuna prossima occorrenza disponibile");
        }

        ScheduledExpense nextOccurrence = new ScheduledExpense(description, amount, type, nextDate);
        nextOccurrence.setRecurrenceType(recurrenceType);
        nextOccurrence.setRecurrenceInterval(recurrenceInterval);
        nextOccurrence.setRecurrenceEndDate(recurrenceEndDate);
        nextOccurrence.setNotes(notes);
        categories.forEach(nextOccurrence::addCategory);

        return nextOccurrence;
    }

    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters/Setters
    public Long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public MovementType getType() { return type; }
    public LocalDate getDueDate() { return dueDate; }
    public RecurrenceType getRecurrenceType() { return recurrenceType; }
    public Integer getRecurrenceInterval() { return recurrenceInterval; }
    public LocalDate getRecurrenceEndDate() { return recurrenceEndDate; }
    public Set<Category> getCategories() { return new HashSet<>(categories); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getNotes() { return notes; }
    public boolean isCompleted() { return completed; }
    public boolean isActive() { return active; }
    public Movement getCreatedMovement() { return createdMovement; }

    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description);
        updateTimestamp();
    }

    public void setAmount(BigDecimal amount) {
        this.amount = validateAmount(amount);
        updateTimestamp();
    }

    public void setType(MovementType type) {
        this.type = Objects.requireNonNull(type);
        updateTimestamp();
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = Objects.requireNonNull(dueDate);
        updateTimestamp();
    }

    public void setRecurrenceType(RecurrenceType recurrenceType) {
        this.recurrenceType = Objects.requireNonNull(recurrenceType);
        updateTimestamp();
    }

    public void setRecurrenceInterval(Integer recurrenceInterval) {
        if (recurrenceInterval != null && recurrenceInterval <= 0) {
            throw new IllegalArgumentException("Intervallo ricorrenza deve essere > 0");
        }
        this.recurrenceInterval = recurrenceInterval;
        updateTimestamp();
    }

    public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
        updateTimestamp();
    }

    public void setNotes(String notes) {
        this.notes = notes;
        updateTimestamp();
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        updateTimestamp();
    }

    public void setActive(boolean active) {
        this.active = active;
        updateTimestamp();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduledExpense)) return false;
        ScheduledExpense that = (ScheduledExpense) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(dueDate, that.dueDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, amount, dueDate);
    }

    @Override
    public String toString() {
        String status = completed ? "Completata" : (isOverdue() ? "Scaduta" : "Attiva");
        return String.format("%s: €%.2f - %s (%s)", description, amount, dueDate, status);
    }
}