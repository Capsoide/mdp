package it.unicam.cs.mpgc.jbudget122631.presentation.controller;

import it.unicam.cs.mpgc.jbudget122631.application.service.ScheduledExpenseService;
import it.unicam.cs.mpgc.jbudget122631.application.service.CategoryService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.ScheduledExpense;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Movement;
import it.unicam.cs.mpgc.jbudget122631.domain.model.RecurrenceType;
import it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType;
import it.unicam.cs.mpgc.jbudget122631.presentation.dialog.AddScheduledExpenseDialog;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.Optional;


public class ScheduledExpenseController implements Initializable {

    @FXML private TableView<ScheduledExpense> scheduledExpensesTable;
    @FXML private TableColumn<ScheduledExpense, String> descriptionColumn;
    @FXML private TableColumn<ScheduledExpense, BigDecimal> amountColumn;
    @FXML private TableColumn<ScheduledExpense, LocalDate> dueDateColumn;
    @FXML private TableColumn<ScheduledExpense, String> recurrenceColumn;
    @FXML private TableColumn<ScheduledExpense, String> statusColumn;
    @FXML private TableColumn<ScheduledExpense, String> daysLeftColumn;

    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<RecurrenceType> recurrenceFilter;
    @FXML private DatePicker dueDateFilter;
    @FXML private Button filterButton;
    @FXML private Button clearFilterButton;
    @FXML private Button addExpenseButton;
    @FXML private Button editExpenseButton;
    @FXML private Button deleteExpenseButton;
    @FXML private Button completeExpenseButton;
    @FXML private Button refreshButton;
    @FXML private Button exportButton;

    @FXML private Label totalExpensesLabel;
    @FXML private Label overdueCountLabel;
    @FXML private Label dueTodayCountLabel;
    @FXML private Label dueThisWeekCountLabel;

    @FXML private VBox overdueExpensesBox;
    @FXML private VBox dueTodayBox;
    @FXML private VBox dueThisWeekBox;

    private final ScheduledExpenseService scheduledExpenseService;
    private final CategoryService categoryService;
    private final ObservableList<ScheduledExpense> scheduledExpenses = FXCollections.observableArrayList();

    public ScheduledExpenseController(ScheduledExpenseService scheduledExpenseService, CategoryService categoryService) {
        this.scheduledExpenseService = scheduledExpenseService;
        this.categoryService = categoryService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        setupButtons();
        loadScheduledExpenses();
        updateSummaryLabels();
        updateQuickActionsBoxes();
    }

    private void setupTable() {
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        recurrenceColumn.setCellValueFactory(cellData -> {
            RecurrenceType type = cellData.getValue().getRecurrenceType();
            String recurrence = type != RecurrenceType.NONE ? type.getDescription() : "Unica";
            Integer interval = cellData.getValue().getRecurrenceInterval();
            if (type != RecurrenceType.NONE && interval != null && interval > 1) {
                recurrence = "Ogni " + interval + " " + type.getDescription().toLowerCase();
            }
            return new SimpleStringProperty(recurrence);
        });

        statusColumn.setCellValueFactory(cellData -> {
            ScheduledExpense expense = cellData.getValue();
            String status;
            if (expense.isCompleted()) {
                status = "Completata";
            } else if (expense.isOverdue()) {
                status = "Scaduta";
            } else if (expense.isDue()) {
                status = "In scadenza oggi";
            } else if (expense.getDaysUntilDue() <= 3) {
                status = "Prossima scadenza";
            } else {
                status = "Attiva";
            }
            return new SimpleStringProperty(status);
        });

        daysLeftColumn.setCellValueFactory(cellData -> {
            ScheduledExpense expense = cellData.getValue();
            long daysLeft = expense.getDaysUntilDue();
            String text;
            if (expense.isCompleted()) {
                text = "Completata";
            } else if (daysLeft < 0) {
                text = Math.abs(daysLeft) + " giorni fa";
            } else if (daysLeft == 0) {
                text = "Oggi";
            } else if (daysLeft == 1) {
                text = "Domani";
            } else {
                text = "Tra " + daysLeft + " giorni";
            }
            return new SimpleStringProperty(text);
        });

        setupMonetaryColumn(amountColumn);
        setupDateColumn(dueDateColumn);
        setupStatusColumn(statusColumn);
        setupDaysLeftColumn(daysLeftColumn);

        scheduledExpensesTable.setItems(scheduledExpenses);
        scheduledExpensesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setupContextMenu();

        scheduledExpensesTable.getSortOrder().add(dueDateColumn);
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Modifica");
        MenuItem deleteItem = new MenuItem("Elimina");
        MenuItem completeItem = new MenuItem("Segna come completata");
        MenuItem viewDetailsItem = new MenuItem("Visualizza dettagli");

        editItem.setOnAction(e -> editSelectedExpense());
        deleteItem.setOnAction(e -> deleteSelectedExpenses());
        completeItem.setOnAction(e -> completeSelectedExpense());
        viewDetailsItem.setOnAction(e -> viewExpenseDetails());

        contextMenu.setOnShowing(e -> {
            ScheduledExpense selected = scheduledExpensesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                completeItem.setDisable(selected.isCompleted());
            }
        });

        contextMenu.getItems().addAll(
                editItem, deleteItem,
                new SeparatorMenuItem(),
                completeItem,
                new SeparatorMenuItem(),
                viewDetailsItem
        );

        scheduledExpensesTable.setContextMenu(contextMenu);
    }

    private void setupMonetaryColumn(TableColumn<ScheduledExpense, BigDecimal> column) {
        column.setCellFactory(col -> new TableCell<ScheduledExpense, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("\u20AC %.2f", amount));

                    ScheduledExpense expense = getTableRow().getItem();
                    if (expense != null) {
                        if (expense.getType() == MovementType.INCOME) {
                            setStyle("-fx-text-fill: green;");
                        } else {
                            setStyle("-fx-text-fill: black;");
                        }
                    }
                }
            }
        });
    }

    private void setupDateColumn(TableColumn<ScheduledExpense, LocalDate> column) {
        column.setCellFactory(col -> new TableCell<ScheduledExpense, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                    // Evidenzia date vicine
                    long daysUntil = LocalDate.now().until(date).getDays();
                    if (daysUntil < 0) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if (daysUntil <= 3) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void setupStatusColumn(TableColumn<ScheduledExpense, String> column) {
        column.setCellFactory(col -> new TableCell<ScheduledExpense, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "Scaduta":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        case "In scadenza oggi":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "Prossima scadenza":
                            setStyle("-fx-text-fill: orange;");
                            break;
                        case "Completata":
                            setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                            break;
                        default:
                            setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });
    }

    private void setupDaysLeftColumn(TableColumn<ScheduledExpense, String> column) {
        column.setCellFactory(col -> new TableCell<ScheduledExpense, String>() {
            @Override
            protected void updateItem(String daysLeft, boolean empty) {
                super.updateItem(daysLeft, empty);
                if (empty || daysLeft == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(daysLeft);
                    if (daysLeft.contains("fa")) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else if ("Oggi".equals(daysLeft) || "Domani".equals(daysLeft)) {
                        setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    } else if ("Completata".equals(daysLeft)) {
                        setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "Tutte", "Solo attive", "In scadenza", "Scadute", "Completate", "Ricorrenti"
            ));
            statusFilter.setValue("Solo attive");
        }

        if (recurrenceFilter != null) {
            recurrenceFilter.setItems(FXCollections.observableArrayList(RecurrenceType.values()));
            recurrenceFilter.setPromptText("Tipo ricorrenza");

            recurrenceFilter.setCellFactory(listView -> new ListCell<RecurrenceType>() {
                @Override
                protected void updateItem(RecurrenceType item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getDescription());
                }
            });

            recurrenceFilter.setButtonCell(new ListCell<RecurrenceType>() {
                @Override
                protected void updateItem(RecurrenceType item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getDescription());
                }
            });
        }

        if (filterButton != null) filterButton.setOnAction(e -> applyFilters());
        if (clearFilterButton != null) clearFilterButton.setOnAction(e -> clearFilters());

        if (statusFilter != null) {
            statusFilter.setOnAction(e -> applyFilters());
        }
        if (recurrenceFilter != null) {
            recurrenceFilter.setOnAction(e -> applyFilters());
        }
        if (dueDateFilter != null) {
            dueDateFilter.setOnAction(e -> applyFilters());
        }
    }

    private void setupButtons() {
        if (addExpenseButton != null) addExpenseButton.setOnAction(e -> addNewExpense());
        if (editExpenseButton != null) editExpenseButton.setOnAction(e -> editSelectedExpense());
        if (deleteExpenseButton != null) deleteExpenseButton.setOnAction(e -> deleteSelectedExpenses());
        if (completeExpenseButton != null) completeExpenseButton.setOnAction(e -> completeSelectedExpense());
        if (refreshButton != null) refreshButton.setOnAction(e -> refreshExpenses());
        if (exportButton != null) exportButton.setOnAction(e -> exportExpenses());

        if (editExpenseButton != null) {
            editExpenseButton.disableProperty().bind(
                    scheduledExpensesTable.getSelectionModel().selectedItemProperty().isNull());
        }
        if (deleteExpenseButton != null) {
            deleteExpenseButton.disableProperty().bind(
                    scheduledExpensesTable.getSelectionModel().selectedItemProperty().isNull());
        }
        if (completeExpenseButton != null) {
            completeExpenseButton.disableProperty().bind(
                    scheduledExpensesTable.getSelectionModel().selectedItemProperty().isNull());
        }
    }
    private void loadScheduledExpenses() {
        try {
            if (scheduledExpenseService != null) {
                List<ScheduledExpense> allExpenses = scheduledExpenseService.getAllScheduledExpenses();

                List<ScheduledExpense> activeExpenses = allExpenses.stream()
                        .filter(expense -> !expense.isCompleted())
                        .collect(Collectors.toList());

                scheduledExpenses.setAll(activeExpenses);
            } else {
                //loadSampleExpenses();
            }
        } catch (Exception e) {
            showError("Errore caricamento spese programmate", e.getMessage());
            e.printStackTrace();
            //loadSampleExpenses();
        }
    }

    private void updateSummaryLabels() {
        try {
            BigDecimal totalActiveAmount = scheduledExpenses.stream()
                    .filter(e -> !e.isCompleted() && e.getType() == MovementType.EXPENSE)
                    .map(ScheduledExpense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long overdueCount = scheduledExpenses.stream()
                    .mapToLong(e -> e.isOverdue() && !e.isCompleted() ? 1 : 0)
                    .sum();

            long dueTodayCount = scheduledExpenses.stream()
                    .mapToLong(e -> e.isDue() && !e.isOverdue() && !e.isCompleted() ? 1 : 0)
                    .sum();

            long dueThisWeekCount = scheduledExpenses.stream()
                    .mapToLong(e -> {
                        long days = e.getDaysUntilDue();
                        return (days >= 0 && days <= 7 && !e.isCompleted()) ? 1 : 0;
                    })
                    .sum();

            if (totalExpensesLabel != null) {
                totalExpensesLabel.setText(String.format("\u20AC %.2f", totalActiveAmount));
            }
            if (overdueCountLabel != null) {
                overdueCountLabel.setText(String.valueOf(overdueCount));
                overdueCountLabel.setStyle(overdueCount > 0 ?
                        "-fx-text-fill: red; -fx-font-weight: bold;" : "-fx-text-fill: green;");
            }
            if (dueTodayCountLabel != null) {
                dueTodayCountLabel.setText(String.valueOf(dueTodayCount));
                dueTodayCountLabel.setStyle(dueTodayCount > 0 ?
                        "-fx-text-fill: orange; -fx-font-weight: bold;" : "-fx-text-fill: green;");
            }
            if (dueThisWeekCountLabel != null) {
                dueThisWeekCountLabel.setText(String.valueOf(dueThisWeekCount));
                dueThisWeekCountLabel.setStyle(dueThisWeekCount > 3 ?
                        "-fx-text-fill: orange;" : "-fx-text-fill: black;");
            }
        } catch (Exception e) {
            System.err.println("Errore aggiornamento summary labels: " + e.getMessage());
        }
    }

    private void updateQuickActionsBoxes() {
        updateOverdueExpensesBox();
        updateDueTodayBox();
        updateDueThisWeekBox();
    }

    private void updateOverdueExpensesBox() {
        if (overdueExpensesBox == null) return;

        try {
            overdueExpensesBox.getChildren().clear();

            List<ScheduledExpense> overdueExpenses = scheduledExpenses.stream()
                    .filter(e -> e.isOverdue() && !e.isCompleted())
                    .sorted((e1, e2) -> e1.getDueDate().compareTo(e2.getDueDate()))
                    .limit(5)
                    .collect(Collectors.toList());

            if (overdueExpenses.isEmpty()) {
                Label noDataLabel = new Label("Nessuna spesa scaduta");
                noDataLabel.setStyle("-fx-text-fill: green; -fx-font-style: italic;");
                overdueExpensesBox.getChildren().add(noDataLabel);
            } else {
                overdueExpenses.forEach(expense -> {
                    Label expenseLabel = new Label(String.format("%s - €%.2f (%d giorni fa)",
                            expense.getDescription(), expense.getAmount(), Math.abs(expense.getDaysUntilDue())));
                    expenseLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

                    Button completeBtn = new Button("Completa");
                    completeBtn.setOnAction(e -> completeExpense(expense));
                    completeBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #ff6b6b; -fx-text-fill: white;");

                    HBox row = new HBox(10);
                    row.getChildren().addAll(expenseLabel, completeBtn);

                    overdueExpensesBox.getChildren().add(row);
                });
            }
        } catch (Exception e) {
            System.err.println("Errore aggiornamento box spese scadute: " + e.getMessage());
        }
    }

    private void updateDueTodayBox() {
        if (dueTodayBox == null) return;

        try {
            dueTodayBox.getChildren().clear();

            List<ScheduledExpense> dueTodayExpenses = scheduledExpenses.stream()
                    .filter(e -> e.isDue() && !e.isOverdue() && !e.isCompleted())
                    .limit(5)
                    .collect(Collectors.toList());

            if (dueTodayExpenses.isEmpty()) {
                Label noDataLabel = new Label("Nessuna spesa in scadenza oggi");
                noDataLabel.setStyle("-fx-text-fill: green; -fx-font-style: italic;");
                dueTodayBox.getChildren().add(noDataLabel);
            } else {
                dueTodayExpenses.forEach(expense -> {
                    Label expenseLabel = new Label(String.format("%s - \u20AC%.2f",
                            expense.getDescription(), expense.getAmount()));
                    expenseLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");

                    Button completeBtn = new Button("Completa");
                    completeBtn.setOnAction(e -> completeExpense(expense));
                    completeBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #ffa726; -fx-text-fill: white;");

                    HBox row = new HBox(10);
                    row.getChildren().addAll(expenseLabel, completeBtn);

                    dueTodayBox.getChildren().add(row);
                });
            }
        } catch (Exception e) {
            System.err.println("Errore aggiornamento box spese oggi: " + e.getMessage());
        }
    }

    private void updateDueThisWeekBox() {
        if (dueThisWeekBox == null) return;

        try {
            dueThisWeekBox.getChildren().clear();

            List<ScheduledExpense> dueThisWeekExpenses = scheduledExpenses.stream()
                    .filter(e -> {
                        long days = e.getDaysUntilDue();
                        return days > 0 && days <= 7 && !e.isCompleted();
                    })
                    .sorted((e1, e2) -> e1.getDueDate().compareTo(e2.getDueDate()))
                    .limit(8)
                    .collect(Collectors.toList());

            if (dueThisWeekExpenses.isEmpty()) {
                Label noDataLabel = new Label("Nessuna spesa in scadenza questa settimana");
                noDataLabel.setStyle("-fx-text-fill: green; -fx-font-style: italic;");
                dueThisWeekBox.getChildren().add(noDataLabel);
            } else {
                dueThisWeekExpenses.forEach(expense -> {
                    Label expenseLabel = new Label(String.format("%s - \u20AC%.2f (tra %d giorni)",
                            expense.getDescription(), expense.getAmount(), expense.getDaysUntilDue()));

                    if (expense.getDaysUntilDue() <= 3) {
                        expenseLabel.setStyle("-fx-text-fill: orange;");
                    }

                    dueThisWeekBox.getChildren().add(expenseLabel);
                });
            }
        } catch (Exception e) {
            System.err.println("Errore aggiornamento box spese settimana: " + e.getMessage());
        }
    }

    private void applyFilters() {
        try {
            if (scheduledExpenseService == null) {
                //loadSampleExpenses();
                return;
            }

            List<ScheduledExpense> filteredExpenses = scheduledExpenseService.getAllScheduledExpenses();

            String statusFilterValue = statusFilter != null ? statusFilter.getValue() : "Solo attive";
            if (statusFilterValue != null && !"Tutte".equals(statusFilterValue)) {
                filteredExpenses = filteredExpenses.stream()
                        .filter(expense -> {
                            switch (statusFilterValue) {
                                case "Tutte":
                                    return true;
                                case "Solo attive":
                                    return !expense.isCompleted() && expense.isActive();
                                case "In scadenza":
                                    return expense.isDue() && !expense.isCompleted();
                                case "Scadute":
                                    return expense.isOverdue() && !expense.isCompleted();
                                case "Completate":
                                    return expense.isCompleted();
                                case "Ricorrenti":
                                    return expense.isRecurring();
                                default:
                                    return !expense.isCompleted();
                            }
                        })
                        .collect(Collectors.toList());
            }

            RecurrenceType recurrenceFilterValue = recurrenceFilter != null ? recurrenceFilter.getValue() : null;
            if (recurrenceFilterValue != null) {
                filteredExpenses = filteredExpenses.stream()
                        .filter(expense -> expense.getRecurrenceType() == recurrenceFilterValue)
                        .collect(Collectors.toList());
            }

            LocalDate dateFilterValue = dueDateFilter != null ? dueDateFilter.getValue() : null;
            if (dateFilterValue != null) {
                filteredExpenses = filteredExpenses.stream()
                        .filter(expense -> expense.getDueDate().equals(dateFilterValue))
                        .collect(Collectors.toList());
            }

            scheduledExpenses.setAll(filteredExpenses);
            updateSummaryLabels();
            updateQuickActionsBoxes();

        } catch (Exception e) {
            showError("Errore applicazione filtri", e.getMessage());
        }
    }

    private void clearFilters() {
        if (statusFilter != null) statusFilter.setValue("Solo attive");
        if (recurrenceFilter != null) recurrenceFilter.setValue(null);
        if (dueDateFilter != null) dueDateFilter.setValue(null);
        loadScheduledExpenses();
        updateSummaryLabels();
        updateQuickActionsBoxes();
    }

    private void addNewExpense() {
        try {
            AddScheduledExpenseDialog dialog = new AddScheduledExpenseDialog(categoryService);
            Optional<ScheduledExpense> result = dialog.showAndWait();

            if (result.isPresent()) {
                ScheduledExpense newExpense = result.get();

                if (scheduledExpenseService != null) {
                    ScheduledExpense savedExpense = scheduledExpenseService.createScheduledExpense(newExpense);

                    applyFilters();

                    showInfo("Spesa Creata",
                            "Spesa programmata '" + savedExpense.getDescription() + "' creata con successo!\n" +
                                    "Scadenza: " + savedExpense.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                    (savedExpense.isRecurring() ? "\nRicorrenza: " + savedExpense.getRecurrenceType().getDescription() : ""));

                } else {
                    scheduledExpenses.add(newExpense);
                    updateSummaryLabels();
                    updateQuickActionsBoxes();

                    showInfo("Spesa Creata",
                            "Spesa programmata '" + newExpense.getDescription() + "' creata in modalita' demo");
                }
            }

        } catch (Exception e) {
            showError("Errore Creazione Spesa",
                    "Si e' verificato un errore durante la creazione della spesa programmata:\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void editSelectedExpense() {
        ScheduledExpense selected = scheduledExpensesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Nessuna Selezione", "Seleziona una spesa programmata da modificare");
            return;
        }

        try {
            AddScheduledExpenseDialog dialog = new AddScheduledExpenseDialog(categoryService);
            dialog.setTitle("Modifica Spesa Programmata");
            dialog.setHeaderText("Modifica i dettagli della spesa programmata");

            dialog.populateFields(selected);
            Optional<ScheduledExpense> result = dialog.showAndWait();

            if (result.isPresent()) {
                ScheduledExpense updatedExpense = result.get();

                if (scheduledExpenseService != null && selected.getId() != null) {
                    scheduledExpenseService.updateScheduledExpense(selected.getId(), updatedExpense);
                    applyFilters();
                    showInfo("Spesa Modificata", "Spesa programmata modificata con successo!");
                } else {
                    int index = scheduledExpenses.indexOf(selected);
                    if (index >= 0) {
                        scheduledExpenses.set(index, updatedExpense);
                        updateSummaryLabels();
                        updateQuickActionsBoxes();
                        showInfo("Spesa Modificata (Demo)", "Spesa programmata modificata in modalita' demo");
                    }
                }
            }

        } catch (Exception e) {
            showError("Errore Modifica Spesa", e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteSelectedExpenses() {
        List<ScheduledExpense> selected = scheduledExpensesTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Conferma eliminazione");
        confirmation.setHeaderText("Eliminare le spese selezionate?");
        confirmation.setContentText(String.format("Verranno eliminate %d spese programmate.\n" +
                "Questa operazione non puo' essere annullata.", selected.size()));

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                for (ScheduledExpense expense : selected) {
                    if (scheduledExpenseService != null) {
                        scheduledExpenseService.deleteScheduledExpense(expense.getId());
                    }
                    scheduledExpenses.remove(expense);
                }
                updateSummaryLabels();
                updateQuickActionsBoxes();
                showInfo("Eliminazione completata",
                        String.format("Eliminate %d spese programmate con successo", selected.size()));
            } catch (Exception e) {
                showError("Errore eliminazione", e.getMessage());
            }
        }
    }

    private void completeSelectedExpense() {
        ScheduledExpense selected = scheduledExpensesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            completeExpense(selected);
        }
    }

    private void completeExpense(ScheduledExpense expense) {
        if (expense.isCompleted()) {
            showInfo("Spesa gia' completata",
                    "La spesa '" + expense.getDescription() + "' e' gia' stata completata");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Conferma completamento");
        confirmation.setHeaderText("Completare la spesa '" + expense.getDescription() + "'?");
        confirmation.setContentText(
                "Importo: €" + expense.getAmount() + "\n" +
                        "Scadenza: " + expense.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n\n" +
                        "Verra' creato un movimento corrispondente.");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                if (scheduledExpenseService != null) {
                    Movement createdMovement = scheduledExpenseService.completeScheduledExpense(expense.getId());

                    applyFilters();
                    updateSummaryLabels();
                    updateQuickActionsBoxes();

                    try {
                        if (it.unicam.cs.mpgc.jbudget122631.infrastructure.config.ApplicationConfig.getMainController() != null) {
                            it.unicam.cs.mpgc.jbudget122631.infrastructure.config.ApplicationConfig.getMainController().refreshAllTabsFromExternal();
                        }
                    } catch (Exception e) {
                        System.err.println("Avviso: impossibile aggiornare altri tab: " + e.getMessage());
                    }

                    String message = "Spesa '" + expense.getDescription() + "' completata con successo";
                    if (createdMovement != null) {
                        message += "\nMovimento creato: €" + createdMovement.getAmount();
                    }
                    /*if (expense.isRecurring()) {
                        message += "\nÈ stata creata automaticamente la prossima occorrenza";
                    }*/

                    showInfo("Spesa completata", message);

                } else {
                    expense.setCompleted(true);
                    applyFilters();
                    updateSummaryLabels();
                    updateQuickActionsBoxes();
                    showInfo("Spesa completata", "Spesa completata in modalita' demo");
                }

            } catch (Exception e) {
                showError("Errore completamento spesa", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void viewExpenseDetails() {
        ScheduledExpense selected = scheduledExpensesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        StringBuilder details = new StringBuilder();
        details.append("DETTAGLI SPESA PROGRAMMATA\n\n");
        details.append("Descrizione: ").append(selected.getDescription()).append("\n");
        details.append("Importo: €").append(selected.getAmount()).append("\n");
        details.append("Tipo: ").append(selected.getType().getDescription()).append("\n");
        details.append("Data scadenza: ").append(selected.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        details.append("Status: ").append(selected.isCompleted() ? "Completata" : "Attiva").append("\n");

        if (selected.isRecurring()) {
            details.append("Ricorrenza: ").append(selected.getRecurrenceType().getDescription());
            if (selected.getRecurrenceInterval() != null && selected.getRecurrenceInterval() > 1) {
                details.append(" (ogni ").append(selected.getRecurrenceInterval()).append(")");
            }
            details.append("\n");

            if (selected.getRecurrenceEndDate() != null) {
                details.append("Fine ricorrenza: ").append(selected.getRecurrenceEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            }

            LocalDate nextDate = selected.getNextDueDate();
            if (nextDate != null) {
                details.append("Prossima scadenza: ").append(nextDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            }
        }

        if (selected.getNotes() != null && !selected.getNotes().trim().isEmpty()) {
            details.append("Note: ").append(selected.getNotes()).append("\n");
        }

        if (!selected.isCompleted()) {
            long daysUntil = selected.getDaysUntilDue();
            if (daysUntil < 0) {
                details.append("\nSCADUTA da ").append(Math.abs(daysUntil)).append(" giorni");
            } else if (daysUntil == 0) {
                details.append("\nSCADE OGGI");
            } else if (daysUntil <= 3) {
                details.append("\nScade tra ").append(daysUntil).append(" giorni");
            }
        }

        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("Dettagli Spesa");
        detailsAlert.setHeaderText(selected.getDescription());
        detailsAlert.setContentText(details.toString());
        detailsAlert.getDialogPane().setPrefWidth(400);
        detailsAlert.showAndWait();
    }

    private void refreshExpenses() {
        applyFilters();
        showInfo("Aggiornamento completato",
                String.format("Refresh di %d spese programmate", scheduledExpenses.size()));
    }

    private void exportExpenses() {
        showInfo("Export Spese Programmate",
                "Funzionalita' di export non ancora implementata.\n\n" +
                        "Formati supportati:\n" +
                        "- CSV (per Excel)\n" +
                        "- PDF (report completo)\n" +
                        "- JSON (backup dati)");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().setPrefWidth(350);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazione");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().setPrefWidth(400);
        alert.showAndWait();
    }

    public ObservableList<ScheduledExpense> getScheduledExpenses() {
        return scheduledExpenses;
    }

    public ScheduledExpenseService getScheduledExpenseService() {
        return scheduledExpenseService;
    }
}