package it.unicam.cs.mpgc.jbudget122631.domain.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
@Table(name = "periods")
public class Period {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String name;

    // Costruttori
    protected Period() {} // JPA

    public Period(String name, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        this.name = Objects.requireNonNull(name, "Nome periodo non può essere null");
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public long getDurationInDays() {
        return java.time.Duration.between(startDate.atStartOfDay(),
                endDate.atStartOfDay()).toDays() + 1;
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Date non possono essere null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Data inizio deve essere <= data fine");
        }
    }

    // Getters/Setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
    }

    // Metodo helper per settare l'ID (utile per i test)
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Period)) return false;
        Period period = (Period) o;

        // Se entrambi hanno un ID, confronta solo l'ID
        if (id != null && period.id != null) {
            return Objects.equals(id, period.id);
        }

        // Altrimenti confronta nome e date
        return Objects.equals(name, period.name) &&
                Objects.equals(startDate, period.startDate) &&
                Objects.equals(endDate, period.endDate);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(name, startDate, endDate);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return String.format("%s (%s - %s)",
                name,
                startDate.format(formatter),
                endDate.format(formatter));
    }
}