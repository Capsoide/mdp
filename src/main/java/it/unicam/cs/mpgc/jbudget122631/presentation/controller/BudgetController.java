package it.unicam.cs.mpgc.jbudget122631.presentation.controller;

import it.unicam.cs.mpgc.jbudget122631.application.dto.BudgetDTO;
import it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.BudgetService;
import it.unicam.cs.mpgc.jbudget122631.application.service.MovementService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;
import java.util.stream.Collectors;

public class BudgetController implements Initializable {

    @FXML private TableView<BudgetDTO> budgetsTable;
    @FXML private TableColumn<BudgetDTO, String> periodColumn;
    @FXML private TableColumn<BudgetDTO, String> categoryColumn;
    @FXML private TableColumn<BudgetDTO, BigDecimal> plannedIncomeColumn;
    @FXML private TableColumn<BudgetDTO, BigDecimal> plannedExpensesColumn;
    @FXML private TableColumn<BudgetDTO, BigDecimal> actualIncomeColumn;
    @FXML private TableColumn<BudgetDTO, BigDecimal> actualExpensesColumn;
    @FXML private TableColumn<BudgetDTO, BigDecimal> varianceColumn;
    @FXML private TableColumn<BudgetDTO, String> statusColumn;

    @FXML private ComboBox<String> periodFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button filterButton;
    @FXML private Button clearFilterButton;
    @FXML private Button addBudgetButton;
    @FXML private Button editBudgetButton;
    @FXML private Button deleteBudgetButton;
    @FXML private Button refreshButton;
    @FXML private Button updateRealValuesButton;

    @FXML private Label totalPlannedIncomeLabel;
    @FXML private Label totalActualIncomeLabel;
    @FXML private Label totalVarianceLabel;
    @FXML private Label overBudgetCountLabel;

    private final BudgetService budgetService;
    private final MovementService movementService;
    private final ObservableList<BudgetDTO> budgets = FXCollections.observableArrayList();

    public BudgetController(BudgetService budgetService, MovementService movementService) {
        this.budgetService = budgetService;
        this.movementService = movementService;
    }

    public BudgetController() {
        this.budgetService = null;
        this.movementService = null;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            System.out.println("BudgetController init START");
            setupTable();
            setupFilters();
            setupButtons();
            loadBudgets();
            System.out.println("BudgetController init COMPLETE");
        } catch (Exception e) {
            System.err.println("Errore inizializzazione BudgetController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTable() {
        periodColumn.setCellValueFactory(new PropertyValueFactory<>("periodName"));
        categoryColumn.setCellValueFactory(cellData -> {
            String categoryName = cellData.getValue().getCategoryName();
            return new SimpleStringProperty(categoryName != null ? categoryName : "N/A");
        });
        plannedIncomeColumn.setCellValueFactory(new PropertyValueFactory<>("plannedIncome"));
        plannedExpensesColumn.setCellValueFactory(new PropertyValueFactory<>("plannedExpenses"));
        actualIncomeColumn.setCellValueFactory(new PropertyValueFactory<>("actualIncome"));
        actualExpensesColumn.setCellValueFactory(new PropertyValueFactory<>("actualExpenses"));
        varianceColumn.setCellValueFactory(new PropertyValueFactory<>("varianceBalance"));

        statusColumn.setCellValueFactory(cellData -> {
            BudgetDTO budget = cellData.getValue();
            String status = budget.isOverBudget() ? "Sforato" : "Nei limiti";
            return new SimpleStringProperty(status);
        });

        setupMonetaryColumn(plannedIncomeColumn);
        setupMonetaryColumn(plannedExpensesColumn);
        setupMonetaryColumn(actualIncomeColumn);
        setupMonetaryColumn(actualExpensesColumn);
        setupVarianceColumn(varianceColumn);
        setupStatusColumn(statusColumn);

        budgetsTable.setItems(budgets);
        budgetsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Modifica");
        MenuItem deleteItem = new MenuItem("Elimina");
        MenuItem refreshItem = new MenuItem("Aggiorna valori reali");

        editItem.setOnAction(e -> editSelectedBudget());
        deleteItem.setOnAction(e -> deleteSelectedBudget());
        refreshItem.setOnAction(e -> updateBudgetWithRealMovements());

        contextMenu.getItems().addAll(editItem, deleteItem, new SeparatorMenuItem(), refreshItem);
        budgetsTable.setContextMenu(contextMenu);
    }

    private void setupMonetaryColumn(TableColumn<BudgetDTO, BigDecimal> column) {
        column.setCellFactory(col -> new TableCell<BudgetDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("€ %.2f", amount));
                }
            }
        });
    }

    private void setupVarianceColumn(TableColumn<BudgetDTO, BigDecimal> column) {
        column.setCellFactory(col -> new TableCell<BudgetDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal variance, boolean empty) {
                super.updateItem(variance, empty);
                if (empty || variance == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("€ %.2f", variance));
                    if (variance.compareTo(BigDecimal.ZERO) >= 0) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("-fx-text-fill: red;");
                    }
                }
            }
        });
    }

    private void setupStatusColumn(TableColumn<BudgetDTO, String> column) {
        column.setCellFactory(col -> new TableCell<BudgetDTO, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Sforato".equals(status)) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: green;");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        periodFilter.setItems(FXCollections.observableArrayList(
                "Tutti i periodi",
                "Gennaio 2025", "Febbraio 2025", "Marzo 2025", "Aprile 2025",
                "Maggio 2025", "Giugno 2025", "Luglio 2025", "Agosto 2025",
                "Settembre 2025", "Ottobre 2025", "Novembre 2025", "Dicembre 2025"
        ));
        periodFilter.setValue("Tutti i periodi");

        categoryFilter.setItems(FXCollections.observableArrayList(
                "Tutte le categorie", "Alimentari", "Trasporti", "Utenze", "Stipendio", "Salute", "Svago"
        ));
        categoryFilter.setValue("Tutte le categorie");

        filterButton.setOnAction(e -> applyFilters());
        clearFilterButton.setOnAction(e -> clearFilters());
    }

    private void setupButtons() {
        addBudgetButton.setOnAction(e -> addNewBudget());
        editBudgetButton.setOnAction(e -> editSelectedBudget());
        deleteBudgetButton.setOnAction(e -> deleteSelectedBudget());
        refreshButton.setOnAction(e -> refreshBudgets());
        if (updateRealValuesButton != null) {
            updateRealValuesButton.setOnAction(e -> updateBudgetWithRealMovements());
        }

        editBudgetButton.disableProperty().bind(
                budgetsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteBudgetButton.disableProperty().bind(
                budgetsTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private void loadBudgets() {
        try {
            System.out.println("BUDGET_CONTROLLER - Inizio caricamento budget...");

            if (budgetService != null) {
                System.out.println("BUDGET_CONTROLLER - Aggiornamento valori reali...");
                budgetService.updateAllBudgetsWithRealMovements();

                List<BudgetDTO> allBudgets = budgetService.getAllBudgets();
                budgets.setAll(allBudgets);

                System.out.println("BUDGET_CONTROLLER - Caricati " + allBudgets.size() + " budget");

                for (BudgetDTO budget : allBudgets) {
                    System.out.println("BUDGET_CONTROLLER - Budget " + budget.getCategoryName() +
                            " (" + budget.getPeriodName() + "): " +
                            "Entrate reali = €" + budget.getActualIncome() +
                            ", Spese reali = €" + budget.getActualExpenses());
                }

                updateSummaryLabels();
                System.out.println("BUDGET_CONTROLLER - Caricamento completato");
            } else {
                System.out.println("BUDGET_CONTROLLER - BudgetService non disponibile");
            }
        } catch (Exception e) {
            System.err.println("BUDGET_CONTROLLER - Errore caricamento budget: " + e.getMessage());
            e.printStackTrace();
            showError("Errore caricamento budget", e.getMessage());
        }
    }

    public void updateBudgetWithRealMovements() {
        if (movementService == null || budgetService == null) {
            System.out.println("Servizi non disponibili - aggiornamento budget saltato");
            return;
        }

        try {
            System.out.println("BUDGET_CONTROLLER - Aggiornamento manuale valori reali...");
            budgetService.updateAllBudgetsWithRealMovements();
            loadBudgets();
        } catch (Exception e) {
            showError("Errore aggiornamento budget", "Errore durante l'aggiornamento dei valori reali: " + e.getMessage());
        }
    }

    private LocalDate getStartDateFromPeriod(String periodName) {
        if (periodName == null) return LocalDate.now().withDayOfMonth(1);

        switch (periodName.toLowerCase()) {
            case "gennaio 2025": return LocalDate.of(2025, 1, 1);
            case "febbraio 2025": return LocalDate.of(2025, 2, 1);
            case "marzo 2025": return LocalDate.of(2025, 3, 1);
            case "aprile 2025": return LocalDate.of(2025, 4, 1);
            case "maggio 2025": return LocalDate.of(2025, 5, 1);
            case "giugno 2025": return LocalDate.of(2025, 6, 1);
            case "luglio 2025": return LocalDate.of(2025, 7, 1);
            case "agosto 2025": return LocalDate.of(2025, 8, 1);
            case "settembre 2025": return LocalDate.of(2025, 9, 1);
            case "ottobre 2025": return LocalDate.of(2025, 10, 1);
            case "novembre 2025": return LocalDate.of(2025, 11, 1);
            case "dicembre 2025": return LocalDate.of(2025, 12, 1);
            default: return LocalDate.now().withDayOfMonth(1);
        }
    }

    private LocalDate getEndDateFromPeriod(String periodName) {
        if (periodName == null) return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        switch (periodName.toLowerCase()) {
            case "gennaio 2025": return LocalDate.of(2025, 1, 31);
            case "febbraio 2025": return LocalDate.of(2025, 2, 28);
            case "marzo 2025": return LocalDate.of(2025, 3, 31);
            case "aprile 2025": return LocalDate.of(2025, 4, 30);
            case "maggio 2025": return LocalDate.of(2025, 5, 31);
            case "giugno 2025": return LocalDate.of(2025, 6, 30);
            case "luglio 2025": return LocalDate.of(2025, 7, 31);
            case "agosto 2025": return LocalDate.of(2025, 8, 31);
            case "settembre 2025": return LocalDate.of(2025, 9, 30);
            case "ottobre 2025": return LocalDate.of(2025, 10, 31);
            case "novembre 2025": return LocalDate.of(2025, 11, 30);
            case "dicembre 2025": return LocalDate.of(2025, 12, 31);
            default: return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        }
    }

    private BigDecimal calculateRealAmountForBudget(List<MovementDTO> movements, String categoryName, MovementType type) {
        if (movements == null || movements.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return movements.stream()
                .filter(movement -> {
                    boolean typeMatch = movement.getType() == type;

                    if (type == MovementType.EXPENSE && movement.getCategoryIds() != null && !movement.getCategoryIds().isEmpty()) {
                        Long categoryId = movement.getCategoryIds().get(0);
                        String movementCategoryName = getCategoryNameById(categoryId);
                        return typeMatch && categoryName.equals(movementCategoryName);
                    }

                    return false;
                })
                .map(MovementDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String getCategoryNameById(Long categoryId) {
        if (categoryId == null) return "N/A";

        switch (categoryId.intValue()) {
            case 1: return "Alimentari";
            case 2: return "Trasporti";
            case 3: return "Utenze";
            case 4: return "Svago";
            case 5: return "Stipendio";
            case 6: return "Salute";
            default: return "N/A";
        }
    }

    private void updateSummaryLabels() {
        BigDecimal totalPlanned = budgets.stream()
                .map(BudgetDTO::getPlannedIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalActual = budgets.stream()
                .collect(Collectors.groupingBy(BudgetDTO::getPeriodName))
                .values()
                .stream()
                .map(budgetsPerPeriod -> {
                    return budgetsPerPeriod.isEmpty() ? BigDecimal.ZERO :
                            budgetsPerPeriod.get(0).getActualIncome();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPlannedExpenses = budgets.stream()
                .map(BudgetDTO::getPlannedExpenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalActualExpenses = budgets.stream()
                .map(BudgetDTO::getActualExpenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal plannedBalance = totalPlanned.subtract(totalPlannedExpenses);
        BigDecimal actualBalance = totalActual.subtract(totalActualExpenses);
        BigDecimal totalVariance = actualBalance.subtract(plannedBalance);

        long overBudgetCount = budgets.stream()
                .mapToLong(b -> b.isOverBudget() ? 1 : 0)
                .sum();

        if (totalPlannedIncomeLabel != null) {
            totalPlannedIncomeLabel.setText(String.format("€ %.2f", totalPlanned));
        }
        if (totalActualIncomeLabel != null) {
            totalActualIncomeLabel.setText(String.format("€ %.2f", totalActual));
        }
        if (totalVarianceLabel != null) {
            totalVarianceLabel.setText(String.format("€ %.2f", totalVariance));
            totalVarianceLabel.setStyle(totalVariance.compareTo(BigDecimal.ZERO) >= 0 ?
                    "-fx-text-fill: green; -fx-font-size: 20; -fx-font-weight: bold;" :
                    "-fx-text-fill: red; -fx-font-size: 20; -fx-font-weight: bold;");
        }
        if (overBudgetCountLabel != null) {
            overBudgetCountLabel.setText(String.valueOf(overBudgetCount));
            overBudgetCountLabel.setStyle(overBudgetCount > 0 ?
                    "-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 20;" :
                    "-fx-text-fill: green; -fx-font-size: 20; -fx-font-weight: bold;");
        }
    }

    private void applyFilters() {
        try {
            List<BudgetDTO> filteredBudgets;

            if (budgetService != null) {
                filteredBudgets = budgetService.getAllBudgets();
            } else {
                filteredBudgets = budgets.stream().collect(Collectors.toList());
            }

            String selectedPeriod = periodFilter.getValue();
            if (selectedPeriod != null && !"Tutti i periodi".equals(selectedPeriod)) {
                filteredBudgets = filteredBudgets.stream()
                        .filter(b -> selectedPeriod.equals(b.getPeriodName()))
                        .collect(Collectors.toList());
            }

            String selectedCategory = categoryFilter.getValue();
            if (selectedCategory != null && !"Tutte le categorie".equals(selectedCategory)) {
                filteredBudgets = filteredBudgets.stream()
                        .filter(b -> selectedCategory.equals(b.getCategoryName()))
                        .collect(Collectors.toList());
            }

            budgets.setAll(filteredBudgets);
            updateSummaryLabels();

        } catch (Exception e) {
            showError("Errore applicazione filtri", e.getMessage());
        }
    }

    private void clearFilters() {
        periodFilter.setValue("Tutti i periodi");
        categoryFilter.setValue("Tutte le categorie");
        loadBudgets();
    }

    public void addNewBudget() {
        try {
            Dialog<BudgetDTO> dialog = new Dialog<>();
            dialog.setTitle("Nuovo Budget");
            dialog.setHeaderText("Crea un nuovo budget per periodo/categoria");

            ButtonType createButtonType = new ButtonType("Crea", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            ComboBox<String> periodCombo = new ComboBox<>();
            periodCombo.setItems(FXCollections.observableArrayList(
                    "Gennaio 2025", "Febbraio 2025", "Marzo 2025", "Aprile 2025",
                    "Maggio 2025", "Giugno 2025", "Luglio 2025", "Agosto 2025",
                    "Settembre 2025", "Ottobre 2025", "Novembre 2025", "Dicembre 2025"));

            ComboBox<String> categoryCombo = new ComboBox<>();
            categoryCombo.setItems(FXCollections.observableArrayList(
                    "Alimentari", "Trasporti", "Utenze", "Svago", "Stipendio", "Salute"));
            categoryCombo.setValue("Alimentari"); // Default cambiato da "Generale" a "Alimentari"

            TextField plannedIncomeField = new TextField("0");
            TextField plannedExpensesField = new TextField("0");
            TextArea notesArea = new TextArea();
            notesArea.setPrefRowCount(2);

            grid.add(new Label("Periodo:"), 0, 0);
            grid.add(periodCombo, 1, 0);
            grid.add(new Label("Categoria:"), 0, 1);
            grid.add(categoryCombo, 1, 1);
            grid.add(new Label("Entrate Pianificate (€):"), 0, 2);
            grid.add(plannedIncomeField, 1, 2);
            grid.add(new Label("Spese Pianificate (€):"), 0, 3);
            grid.add(plannedExpensesField, 1, 3);
            grid.add(new Label("Note:"), 0, 4);
            grid.add(notesArea, 1, 4);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == createButtonType) {
                    try {
                        BudgetDTO dto = new BudgetDTO();
                        dto.setPeriodName(periodCombo.getValue());
                        dto.setCategoryName(categoryCombo.getValue());
                        dto.setPlannedIncome(new BigDecimal(plannedIncomeField.getText()));
                        dto.setPlannedExpenses(new BigDecimal(plannedExpensesField.getText()));
                        dto.setNotes(notesArea.getText());
                        return dto;
                    } catch (NumberFormatException e) {
                        showError("Errore formato", "Importi non validi");
                        return null;
                    }
                }
                return null;
            });

            Optional<BudgetDTO> result = dialog.showAndWait();
            result.ifPresent(budgetDTO -> {
                try {
                    if (budgetService != null) {
                        budgetService.createBudget(budgetDTO);
                        loadBudgets();
                    } else {
                        budgetDTO.setId(System.currentTimeMillis());
                        budgetDTO.setActualIncome(BigDecimal.ZERO);
                        budgetDTO.setActualExpenses(BigDecimal.ZERO);
                        budgets.add(budgetDTO);
                        updateBudgetWithRealMovements();
                    }
                    updateSummaryLabels();
                    showInfo("Budget Creato", "Budget creato con successo!");
                } catch (Exception e) {
                    showError("Errore creazione budget", e.getMessage());
                }
            });

        } catch (Exception e) {
            showError("Errore dialog", e.getMessage());
        }
    }

    private void editSelectedBudget() {
        BudgetDTO selected = budgetsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Nessun budget selezionato", "Seleziona un budget dalla tabella per modificarlo.");
            return;
        }

        try {
            Dialog<BudgetDTO> dialog = new Dialog<>();
            dialog.setTitle("Modifica Budget");
            dialog.setHeaderText("Modifica budget " + selected.getCategoryName() + " - " + selected.getPeriodName());

            ButtonType saveButtonType = new ButtonType("Salva", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            Label periodLabel = new Label(selected.getPeriodName());
            Label categoryLabel = new Label(selected.getCategoryName());

            TextField plannedIncomeField = new TextField(selected.getPlannedIncome().toString());
            TextField plannedExpensesField = new TextField(selected.getPlannedExpenses().toString());
            TextArea notesArea = new TextArea(selected.getNotes() != null ? selected.getNotes() : "");
            notesArea.setPrefRowCount(2);

            grid.add(new Label("Periodo:"), 0, 0);
            grid.add(periodLabel, 1, 0);
            grid.add(new Label("Categoria:"), 0, 1);
            grid.add(categoryLabel, 1, 1);
            grid.add(new Label("Entrate Pianificate (€):"), 0, 2);
            grid.add(plannedIncomeField, 1, 2);
            grid.add(new Label("Spese Pianificate (€):"), 0, 3);
            grid.add(plannedExpensesField, 1, 3);
            grid.add(new Label("Note:"), 0, 4);
            grid.add(notesArea, 1, 4);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        BudgetDTO dto = new BudgetDTO();
                        dto.setId(selected.getId());
                        dto.setPeriodName(selected.getPeriodName());
                        dto.setCategoryName(selected.getCategoryName());
                        dto.setPlannedIncome(new BigDecimal(plannedIncomeField.getText()));
                        dto.setPlannedExpenses(new BigDecimal(plannedExpensesField.getText()));
                        dto.setNotes(notesArea.getText());
                        return dto;
                    } catch (NumberFormatException e) {
                        showError("Errore formato", "Importi non validi");
                        return null;
                    }
                }
                return null;
            });

            Optional<BudgetDTO> result = dialog.showAndWait();
            result.ifPresent(budgetDTO -> {
                try {
                    if (budgetService != null) {
                        budgetService.updateBudget(budgetDTO.getId(), budgetDTO);
                        loadBudgets();
                    } else {
                        int index = budgets.indexOf(selected);
                        if (index >= 0) {
                            selected.setPlannedIncome(budgetDTO.getPlannedIncome());
                            selected.setPlannedExpenses(budgetDTO.getPlannedExpenses());
                            selected.setNotes(budgetDTO.getNotes());
                            budgets.set(index, selected);
                        }
                    }
                    updateSummaryLabels();
                    showInfo("Budget Modificato", "Budget modificato con successo!");
                } catch (Exception e) {
                    showError("Errore modifica budget", e.getMessage());
                }
            });

        } catch (Exception e) {
            showError("Errore dialog", e.getMessage());
        }
    }

    private void deleteSelectedBudget() {
        BudgetDTO selected = budgetsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Conferma eliminazione");
        confirmation.setHeaderText("Eliminare il budget selezionato?");
        confirmation.setContentText("Questa operazione non puo' essere annullata.");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                if (budgetService != null) {
                    budgetService.deleteBudget(selected.getId());
                    loadBudgets();
                } else {
                    budgets.remove(selected);
                }
                updateSummaryLabels();
                showInfo("Eliminazione completata", "Budget eliminato con successo");
            } catch (Exception e) {
                showError("Errore eliminazione", e.getMessage());
            }
        }
    }

    private void refreshBudgets() {
        loadBudgets();
        updateSummaryLabels();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazione");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}