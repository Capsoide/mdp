package it.unicam.cs.mpgc.jbudget122631.application.dto;

import it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MovementDTO {
    private Long id;
    private String description;
    private BigDecimal amount;
    private MovementType type;
    private LocalDate date;
    private List<Long> categoryIds;
    private String notes;
    private boolean scheduled;
    public MovementDTO() {
        this.categoryIds = new ArrayList<>();
        this.scheduled = false;
    }
    public MovementDTO(String description, BigDecimal amount, MovementType type, LocalDate date) {
        this();
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.date = date;
    }
    private BigDecimal nullSafeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
    private List<Long> nullSafeList(List<Long> list) {
        return list != null ? list : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return nullSafeBigDecimal(amount);
    }

    public void setAmount(BigDecimal amount) {
        this.amount = nullSafeBigDecimal(amount);
    }

    public MovementType getType() {
        return type;
    }

    public void setType(MovementType type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<Long> getCategoryIds() {
        return nullSafeList(categoryIds);
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = nullSafeList(categoryIds);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovementDTO)) return false;
        MovementDTO that = (MovementDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("MovementDTO{id=%s, description='%s', amount=%s, type=%s, date=%s, categoryIds=%s, notes='%s', scheduled=%s}",
                Objects.toString(id, "null"),
                Objects.toString(description, ""),
                Objects.toString(getAmount(), "0"),
                Objects.toString(type, "null"),
                Objects.toString(date, "null"),
                Objects.toString(getCategoryIds(), "[]"),
                Objects.toString(notes, ""),
                scheduled);
    }
}