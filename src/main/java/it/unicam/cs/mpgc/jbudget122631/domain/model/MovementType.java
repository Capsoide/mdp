// MovementType.java
package it.unicam.cs.mpgc.jbudget122631.domain.model;

public enum MovementType {
    INCOME("Entrata"),
    EXPENSE("Uscita");

    private final String description;

    MovementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}