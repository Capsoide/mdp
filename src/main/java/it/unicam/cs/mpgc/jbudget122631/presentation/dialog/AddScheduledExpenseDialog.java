package it.unicam.cs.mpgc.jbudget122631.presentation.dialog;

import it.unicam.cs.mpgc.jbudget122631.domain.model.*;
import it.unicam.cs.mpgc.jbudget122631.application.service.CategoryService;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;

public class AddScheduledExpenseDialog extends Dialog<ScheduledExpense> {

    private TextField descriptionField;
    private TextField amountField;
    private ComboBox<MovementType> typeComboBox;
    private DatePicker dueDatePicker;
    private ComboBox<RecurrenceType> recurrenceTypeComboBox;
    private TextField recurrenceIntervalField;
    private DatePicker recurrenceEndDatePicker;
    private TextArea notesArea;
    private ListView<Category> categoriesListView;

    private final CategoryService categoryService;

    public AddScheduledExpenseDialog(CategoryService categoryService) {
        this.categoryService = categoryService;
        initializeDialog();
        createLayout();
        setupValidation();
        setupResultConverter();
    }

    private void initializeDialog() {
        setTitle("Nuova Spesa Programmata");
        setHeaderText("Inserisci i dettagli della spesa programmata");

        // Bottoni
        ButtonType createButtonType = new ButtonType("Crea", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Dimensioni
        getDialogPane().setPrefWidth(500);
        getDialogPane().setPrefHeight(600);
    }

    private void createLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // AGGIUNGI QUESTE RIGHE PER GESTIRE LE COLONNE
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(120);  // Larghezza minima per le etichette
        col1.setPrefWidth(120); // Larghezza preferita per le etichette

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS); // La seconda colonna si espande
        col2.setMinWidth(250);

        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // Descrizione (obbligatorio)
        Label descriptionLabel = new Label("*Descrizione:");
        descriptionLabel.setStyle("-fx-font-weight: bold;");
        descriptionField = new TextField();
        descriptionField.setPromptText("Es: Bolletta della luce");
        descriptionField.setPrefWidth(250); // Larghezza fissa per i TextField
        grid.add(descriptionLabel, 0, row);
        grid.add(descriptionField, 1, row++);

        // Importo (obbligatorio)
        Label amountLabel = new Label("*Importo (€):");
        amountLabel.setStyle("-fx-font-weight: bold;");
        amountField = new TextField();
        amountField.setPromptText("Es: 150.50");
        amountField.setPrefWidth(250);
        grid.add(amountLabel, 0, row);
        grid.add(amountField, 1, row++);

        // Tipo movimento
        Label typeLabel = new Label("Tipo:");
        typeComboBox = new ComboBox<>(FXCollections.observableArrayList(MovementType.values()));
        typeComboBox.setValue(MovementType.EXPENSE);
        typeComboBox.setPrefWidth(250);
        typeComboBox.setConverter(new StringConverter<MovementType>() {
            @Override
            public String toString(MovementType type) {
                return type != null ? type.getDescription() : "";
            }
            @Override
            public MovementType fromString(String string) {
                return MovementType.EXPENSE;
            }
        });
        grid.add(typeLabel, 0, row);
        grid.add(typeComboBox, 1, row++);

        // Data scadenza (obbligatorio)
        Label dueDateLabel = new Label("*Data Scadenza:");
        dueDateLabel.setStyle("-fx-font-weight: bold;");
        dueDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        dueDatePicker.setPrefWidth(250);
        grid.add(dueDateLabel, 0, row);
        grid.add(dueDatePicker, 1, row++);

        // Separatore ricorrenza
        Separator separator = new Separator();
        grid.add(separator, 0, row, 2, 1);
        row++;

        // Tipo ricorrenza
        Label recurrenceLabel = new Label("Ricorrenza:");
        recurrenceTypeComboBox = new ComboBox<>(FXCollections.observableArrayList(RecurrenceType.values()));
        recurrenceTypeComboBox.setValue(RecurrenceType.NONE);
        recurrenceTypeComboBox.setPrefWidth(250);
        recurrenceTypeComboBox.setConverter(new StringConverter<RecurrenceType>() {
            @Override
            public String toString(RecurrenceType type) {
                return type != null ? type.getDescription() : "";
            }
            @Override
            public RecurrenceType fromString(String string) {
                return RecurrenceType.NONE;
            }
        });
        grid.add(recurrenceLabel, 0, row);
        grid.add(recurrenceTypeComboBox, 1, row++);

        // Intervallo ricorrenza
        Label intervalLabel = new Label("Ogni (n):");
        recurrenceIntervalField = new TextField("1");
        recurrenceIntervalField.setPromptText("1");
        recurrenceIntervalField.setPrefWidth(60);
        Label intervalHelpLabel = new Label("(es: ogni 2 mesi)");
        intervalHelpLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        HBox intervalBox = new HBox(5);
        intervalBox.getChildren().addAll(recurrenceIntervalField, intervalHelpLabel);

        grid.add(intervalLabel, 0, row);
        grid.add(intervalBox, 1, row++);

        // Data fine ricorrenza
        Label endDateLabel = new Label("Fine Ricorrenza:");
        recurrenceEndDatePicker = new DatePicker();
        recurrenceEndDatePicker.setPromptText("Opzionale - lascia vuoto per illimitata");
        recurrenceEndDatePicker.setPrefWidth(250);
        grid.add(endDateLabel, 0, row);
        grid.add(recurrenceEndDatePicker, 1, row++);

        // Categorie
        Label categoriesLabel = new Label("Categorie:");
        categoriesListView = new ListView<>();
        categoriesListView.setPrefHeight(80);
        categoriesListView.setPrefWidth(250);
        categoriesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        try {
            if (categoryService != null) {
                categoriesListView.setItems(FXCollections.observableArrayList(categoryService.getAllCategories()));
            }
        } catch (Exception e) {
            // Se il servizio categorie non è disponibile, lascia vuoto
            System.err.println("Servizio categorie non disponibile: " + e.getMessage());
        }

        grid.add(categoriesLabel, 0, row);
        grid.add(categoriesListView, 1, row++);

        // Note
        Label notesLabel = new Label("Note:");
        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setPrefWidth(250);
        notesArea.setPromptText("Note opzionali sulla spesa...");
        grid.add(notesLabel, 0, row);
        grid.add(notesArea, 1, row++);

        // Info sui campi obbligatori
        Label requiredInfo = new Label("* Campi obbligatori");
        requiredInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: red;");
        grid.add(requiredInfo, 0, row, 2, 1);

        // Gestione visibilità campi ricorrenza
        setupRecurrenceVisibility();

        getDialogPane().setContent(grid);
    }

    private void setupRecurrenceVisibility() {
        // Mostra/nasconde campi ricorrenza in base alla selezione
        recurrenceTypeComboBox.setOnAction(e -> {
            boolean isRecurring = recurrenceTypeComboBox.getValue() != RecurrenceType.NONE;
            recurrenceIntervalField.setDisable(!isRecurring);
            recurrenceEndDatePicker.setDisable(!isRecurring);

            if (!isRecurring) {
                recurrenceIntervalField.setText("1");
                recurrenceEndDatePicker.setValue(null);
            }
        });

        // Inizialmente disabilita i campi ricorrenza
        recurrenceIntervalField.setDisable(true);
        recurrenceEndDatePicker.setDisable(true);
    }

    private void setupValidation() {
        // Disabilita il pulsante Crea se i campi obbligatori sono vuoti
        Button createButton = (Button) getDialogPane().lookupButton(
                getDialogPane().getButtonTypes().stream()
                        .filter(buttonType -> buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst()
                        .orElse(null)
        );

        if (createButton != null) {
            // Validazione in tempo reale
            createButton.disableProperty().bind(
                    descriptionField.textProperty().isEmpty()
                            .or(amountField.textProperty().isEmpty())
                            .or(dueDatePicker.valueProperty().isNull())
            );
        }

        // Validazione formato importo
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldVal);
            }
        });

        // Validazione formato intervallo ricorrenza
        recurrenceIntervalField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                recurrenceIntervalField.setText(oldVal);
            }
        });
    }

    private void setupResultConverter() {
        setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    return createScheduledExpense();
                } catch (Exception e) {
                    showValidationError(e.getMessage());
                    return null;
                }
            }
            return null;
        });
    }

    private ScheduledExpense createScheduledExpense() throws Exception {
        // Validazione campi obbligatori
        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Descrizione è obbligatoria");
        }

        String amountText = amountField.getText().trim();
        if (amountText.isEmpty()) {
            throw new IllegalArgumentException("Importo è obbligatorio");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Importo deve essere maggiore di zero");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato importo non valido");
        }

        LocalDate dueDate = dueDatePicker.getValue();
        if (dueDate == null) {
            throw new IllegalArgumentException("Data scadenza è obbligatoria");
        }

        // Crea la spesa programmata
        MovementType type = typeComboBox.getValue();
        ScheduledExpense scheduledExpense = new ScheduledExpense(description, amount, type, dueDate);

        // Imposta ricorrenza se specificata
        RecurrenceType recurrenceType = recurrenceTypeComboBox.getValue();
        if (recurrenceType != RecurrenceType.NONE) {
            scheduledExpense.setRecurrenceType(recurrenceType);

            String intervalText = recurrenceIntervalField.getText().trim();
            if (!intervalText.isEmpty()) {
                try {
                    int interval = Integer.parseInt(intervalText);
                    if (interval <= 0) {
                        throw new IllegalArgumentException("Intervallo ricorrenza deve essere maggiore di zero");
                    }
                    scheduledExpense.setRecurrenceInterval(interval);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Formato intervallo ricorrenza non valido");
                }
            }

            LocalDate endDate = recurrenceEndDatePicker.getValue();
            if (endDate != null) {
                if (endDate.isBefore(dueDate)) {
                    throw new IllegalArgumentException("Data fine ricorrenza non può essere prima della scadenza");
                }
                scheduledExpense.setRecurrenceEndDate(endDate);
            }
        }

        // Aggiungi categorie selezionate
        Set<Category> selectedCategories = new HashSet<>(categoriesListView.getSelectionModel().getSelectedItems());
        selectedCategories.forEach(scheduledExpense::addCategory);

        // Imposta note
        String notes = notesArea.getText().trim();
        if (!notes.isEmpty()) {
            scheduledExpense.setNotes(notes);
        }

        return scheduledExpense;
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore Validazione");
        alert.setHeaderText("Dati non validi");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Metodo per precompilare il form (utile per la modifica)
    public void populateFields(ScheduledExpense expense) {
        if (expense == null) return;

        descriptionField.setText(expense.getDescription());
        amountField.setText(expense.getAmount().toString());
        typeComboBox.setValue(expense.getType());
        dueDatePicker.setValue(expense.getDueDate());
        recurrenceTypeComboBox.setValue(expense.getRecurrenceType());

        if (expense.getRecurrenceInterval() != null) {
            recurrenceIntervalField.setText(expense.getRecurrenceInterval().toString());
        }

        recurrenceEndDatePicker.setValue(expense.getRecurrenceEndDate());

        if (expense.getNotes() != null) {
            notesArea.setText(expense.getNotes());
        }

        // Seleziona le categorie
        if (categoriesListView.getItems() != null) {
            expense.getCategories().forEach(category -> {
                int index = categoriesListView.getItems().indexOf(category);
                if (index >= 0) {
                    categoriesListView.getSelectionModel().select(index);
                }
            });
        }
    }
}