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

        ButtonType createButtonType = new ButtonType("Crea", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        getDialogPane().setPrefWidth(500);
        getDialogPane().setPrefHeight(600);
    }

    private void createLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(120);
        col1.setPrefWidth(120);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setMinWidth(250);

        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        Label descriptionLabel = new Label("*Descrizione:");
        descriptionLabel.setStyle("-fx-font-weight: bold;");
        descriptionField = new TextField();
        descriptionField.setPromptText("Es: Bolletta della luce");
        descriptionField.setPrefWidth(250); // Larghezza fissa per i TextField
        grid.add(descriptionLabel, 0, row);
        grid.add(descriptionField, 1, row++);

        Label amountLabel = new Label("*Importo (€):");
        amountLabel.setStyle("-fx-font-weight: bold;");
        amountField = new TextField();
        amountField.setPromptText("Es: 150.50");
        amountField.setPrefWidth(250);
        grid.add(amountLabel, 0, row);
        grid.add(amountField, 1, row++);

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

        Label dueDateLabel = new Label("*Data Scadenza:");
        dueDateLabel.setStyle("-fx-font-weight: bold;");
        dueDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        dueDatePicker.setPrefWidth(250);
        grid.add(dueDateLabel, 0, row);
        grid.add(dueDatePicker, 1, row++);

        Separator separator = new Separator();
        grid.add(separator, 0, row, 2, 1);
        row++;

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

        Label endDateLabel = new Label("Fine Ricorrenza:");
        recurrenceEndDatePicker = new DatePicker();
        recurrenceEndDatePicker.setPromptText("Opzionale - lascia vuoto per illimitata");
        recurrenceEndDatePicker.setPrefWidth(250);
        grid.add(endDateLabel, 0, row);
        grid.add(recurrenceEndDatePicker, 1, row++);

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
            System.err.println("Servizio categorie non disponibile: " + e.getMessage());
        }

        grid.add(categoriesLabel, 0, row);
        grid.add(categoriesListView, 1, row++);

        Label notesLabel = new Label("Note:");
        notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setPrefWidth(250);
        notesArea.setPromptText("Note opzionali sulla spesa...");
        grid.add(notesLabel, 0, row);
        grid.add(notesArea, 1, row++);

        Label requiredInfo = new Label("* Campi obbligatori");
        requiredInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: red;");
        grid.add(requiredInfo, 0, row, 2, 1);

        setupRecurrenceVisibility();

        getDialogPane().setContent(grid);
    }

    private void setupRecurrenceVisibility() {
        recurrenceTypeComboBox.setOnAction(e -> {
            boolean isRecurring = recurrenceTypeComboBox.getValue() != RecurrenceType.NONE;
            recurrenceIntervalField.setDisable(!isRecurring);
            recurrenceEndDatePicker.setDisable(!isRecurring);

            if (!isRecurring) {
                recurrenceIntervalField.setText("1");
                recurrenceEndDatePicker.setValue(null);
            }
        });

        recurrenceIntervalField.setDisable(true);
        recurrenceEndDatePicker.setDisable(true);
    }

    private void setupValidation() {
        Button createButton = (Button) getDialogPane().lookupButton(
                getDialogPane().getButtonTypes().stream()
                        .filter(buttonType -> buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                        .findFirst()
                        .orElse(null)
        );

        if (createButton != null) {
            createButton.disableProperty().bind(
                    descriptionField.textProperty().isEmpty()
                            .or(amountField.textProperty().isEmpty())
                            .or(dueDatePicker.valueProperty().isNull())
            );
        }

        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldVal);
            }
        });

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

        MovementType type = typeComboBox.getValue();
        ScheduledExpense scheduledExpense = new ScheduledExpense(description, amount, type, dueDate);

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

        Set<Category> selectedCategories = new HashSet<>(categoriesListView.getSelectionModel().getSelectedItems());
        selectedCategories.forEach(scheduledExpense::addCategory);

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