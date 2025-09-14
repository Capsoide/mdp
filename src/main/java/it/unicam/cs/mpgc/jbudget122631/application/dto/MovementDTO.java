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

    /**
     * Costruttore di default che inizializza le collezioni per evitare null pointer
     */
    public MovementDTO() {
        this.categoryIds = new ArrayList<>();
        this.scheduled = false;
    }

    /**
     * Costruttore parametrizzato per creare un movimento con i dati essenziali
     * Inizializza automaticamente le collezioni e i valori di default
     * @param description la descrizione del movimento
     * @param amount l'importo del movimento
     * @param type il tipo di movimento (INCOME o EXPENSE)
     * @param date la data del movimento
     */
    public MovementDTO(String description, BigDecimal amount, MovementType type, LocalDate date) {
        this();
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.date = date;
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
     * Metodo helper per gestire liste null restituendo una lista vuota
     * @param list la lista da verificare
     * @return la lista originale se non null, altrimenti una lista vuota
     */
    private List<Long> nullSafeList(List<Long> list) {
        return list != null ? list : new ArrayList<>();
    }

    /**
     * Restituisce l'identificativo univoco del movimento
     * @return l'ID del movimento
     */
    public Long getId() {
        return id;
    }

    /**
     * Imposta l'identificativo univoco del movimento
     * @param id l'ID da assegnare al movimento
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Restituisce la descrizione del movimento
     * @return la descrizione testuale del movimento
     */
    public String getDescription() {
        return description;
    }

    /**
     * Imposta la descrizione del movimento
     * @param description la descrizione testuale da assegnare
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Restituisce l'importo del movimento garantendo un valore non null
     * @return l'importo del movimento o zero se null
     */
    public BigDecimal getAmount() {
        return nullSafeBigDecimal(amount);
    }

    /**
     * Imposta l'importo del movimento con protezione da valori null
     * @param amount l'importo da assegnare al movimento
     */
    public void setAmount(BigDecimal amount) {
        this.amount = nullSafeBigDecimal(amount);
    }

    /**
     * Restituisce il tipo di movimento (entrata o uscita)
     * @return il tipo del movimento
     */
    public MovementType getType() {
        return type;
    }

    /**
     * Imposta il tipo di movimento
     * @param type il tipo da assegnare (INCOME o EXPENSE)
     */
    public void setType(MovementType type) {
        this.type = type;
    }

    /**
     * Restituisce la data del movimento
     * @return la data in cui è avvenuto il movimento
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Imposta la data del movimento
     * @param date la data da assegnare al movimento
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }

    /**
     * Restituisce gli identificativi delle categorie associate garantendo una lista non null
     * @return la lista degli ID delle categorie o una lista vuota se null
     */
    public List<Long> getCategoryIds() {
        return nullSafeList(categoryIds);
    }

    /**
     * Imposta gli identificativi delle categorie associate con protezione da valori null
     * @param categoryIds la lista degli ID delle categorie da associare
     */
    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = nullSafeList(categoryIds);
    }

    /**
     * Restituisce le note aggiuntive del movimento
     * @return le note descrittive del movimento
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Imposta le note aggiuntive per il movimento
     * @param notes le note descrittive da assegnare
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Verifica se il movimento è programmato per il futuro
     * @return true se il movimento è schedulato, false altrimenti
     */
    public boolean isScheduled() {
        return scheduled;
    }

    /**
     * Imposta se il movimento è programmato
     * @param scheduled true per indicare che il movimento è schedulato
     */
    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    /**
     * Confronta due oggetti MovementDTO per uguaglianza basandosi sull'ID
     * @param o l'oggetto da confrontare
     * @return true se gli oggetti hanno lo stesso ID
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovementDTO)) return false;
        MovementDTO that = (MovementDTO) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Genera il codice hash basato sull'ID univoco dell'oggetto
     * @return il codice hash calcolato sull'ID
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Restituisce una rappresentazione testuale del movimento con protezione da valori null
     * @return una stringa che descrive il movimento con tutti i suoi attributi
     */
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