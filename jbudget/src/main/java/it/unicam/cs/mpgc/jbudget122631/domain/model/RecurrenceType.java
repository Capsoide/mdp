package it.unicam.cs.mpgc.jbudget122631.domain.model;

public enum RecurrenceType {
    NONE("Nessuna"),
    DAILY("Giornaliera"),
    WEEKLY("Settimanale"),
    MONTHLY("Mensile"),
    YEARLY("Annuale");

    private final String description;

    RecurrenceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}