package it.unicam.cs.mpgc.jbudget122631.presentation.controller;

import it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.CategoryService;
import it.unicam.cs.mpgc.jbudget122631.application.service.MovementService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.Optional;

public class MovementController implements Initializable {

    @FXML
    private TableView<MovementDTO> movementsTable;
    @FXML
    private TableColumn<MovementDTO, LocalDate> dateColumn;
    @FXML
    private TableColumn<MovementDTO, String> descriptionColumn;
    @FXML
    private TableColumn<MovementDTO, String> typeColumn;
    @FXML
    private TableColumn<MovementDTO, BigDecimal> amountColumn;
    @FXML
    private TableColumn<MovementDTO, String> categoriesColumn;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<MovementType> typeFilter;
    @FXML
    private DatePicker startDateFilter;
    @FXML
    private DatePicker endDateFilter;
    @FXML
    private Button filterButton;
    @FXML
    private Button clearFilterButton;
    @FXML
    private Button addMovementButton;
    @FXML
    private Button editMovementButton;
    @FXML
    private Button deleteMovementButton;
    @FXML
    private Label totalMovementsLabel;

    private final MovementService movementService;
    private BudgetController budgetController;
    private CategoryService categoryService;
    private final ObservableList<MovementDTO> movements = FXCollections.observableArrayList();

    public MovementController(MovementService movementService, CategoryService categoryService) {
        this.movementService = movementService;
        this.categoryService = categoryService;
    }

    // Costruttore di default per FXML
    public MovementController() {
        this.movementService = null;
    }

    // Metodo per impostare il riferimento al BudgetController
    public void setBudgetController(BudgetController budgetController) {
        this.budgetController = budgetController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilters();
        setupButtons();
        loadMovements();

        Platform.runLater(() -> {
            movementsTable.refresh();
            System.out.println("Tabella aggiornata - movimenti visibili: " + movements.size());
        });
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getType().getDescription()));

        categoriesColumn.setCellValueFactory(cellData -> {
            List<Long> categoryIds = cellData.getValue().getCategoryIds();
            if (categoryIds == null || categoryIds.isEmpty()) {
                return new SimpleStringProperty("-");
            }

            if (categoryService != null) {
                String categories = categoryIds.stream()
                        .map(id -> categoryService.getCategoryById(id))
                        .filter(opt -> opt.isPresent())
                        .map(opt -> opt.get().getName())
                        .collect(Collectors.joining(", "));
                return new SimpleStringProperty(categories);
            } else {
                String categories = categoryIds.stream()
                        .map(id -> getCategoryNameById(id))
                        .collect(Collectors.joining(", "));
                return new SimpleStringProperty(categories);
            }
        });

        amountColumn.setCellFactory(column -> new TableCell<MovementDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("€ %.2f", amount));

                    MovementDTO movement = getTableRow().getItem();
                    if (movement != null) {
                        if (movement.getType() == MovementType.INCOME) {
                            setStyle("-fx-text-fill: green;");
                        } else if (movement.getType() == MovementType.EXPENSE) {
                            setStyle("-fx-text-fill: red;");
                        } else {
                            setStyle("-fx-text-fill: blue;");
                        }
                    }
                }
            }
        });

        movementsTable.setItems(movements);
        movementsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Modifica");
        MenuItem deleteItem = new MenuItem("Elimina");
        editItem.setOnAction(e -> editSelectedMovement());
        deleteItem.setOnAction(e -> deleteSelectedMovements());
        contextMenu.getItems().addAll(editItem, deleteItem);
        movementsTable.setContextMenu(contextMenu);
    }

    private String getCategoryNameById(Long id) {
        switch (id.intValue()) {
            case 1: return "Alimentari";
            case 2: return "Trasporti";
            case 3: return "Utenze";
            case 4: return "Svago";
            case 5: return "Stipendio";
            case 6: return "Salute";
            default: return "Generale";
        }
    }

    private void setupFilters() {
        typeFilter.setItems(FXCollections.observableArrayList(MovementType.INCOME, MovementType.EXPENSE));
        typeFilter.setPromptText("Tutti i tipi");

        endDateFilter.setValue(LocalDate.now());
        startDateFilter.setValue(LocalDate.now().minusMonths(1));

        filterButton.setOnAction(e -> applyFilters());
        clearFilterButton.setOnAction(e -> clearFilters());
        searchField.setOnAction(e -> applyFilters());
    }

    private void setupButtons() {
        addMovementButton.setOnAction(e -> showAddNewMovementDialog());
        editMovementButton.setOnAction(e -> editSelectedMovement());
        deleteMovementButton.setOnAction(e -> deleteSelectedMovements());

        editMovementButton.disableProperty().bind(
                movementsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteMovementButton.disableProperty().bind(
                movementsTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private void loadMovements() {
        try {
            if (movementService != null) {
                List<MovementDTO> allMovements = movementService.getAllMovements();
                movements.setAll(allMovements);
            }
            updateTotalMovementsLabel();
        } catch (Exception e) {
            showError("Errore caricamento movimenti", e.getMessage());
        }
    }

    private void updateTotalMovementsLabel() {
        if (totalMovementsLabel != null) {
            totalMovementsLabel.setText(String.valueOf(movements.size()));
        }
    }

    private void applyFilters() {
        try {
            List<MovementDTO> filteredMovements;

            LocalDate startDate = startDateFilter.getValue();
            LocalDate endDate = endDateFilter.getValue();

            if (startDate != null && endDate != null && movementService != null) {
                filteredMovements = movementService.getMovementsByDateRange(startDate, endDate);
            } else if (movementService != null) {
                filteredMovements = movementService.getAllMovements();
            } else {
                filteredMovements = movements.stream().collect(Collectors.toList());

                if (startDate != null && endDate != null) {
                    filteredMovements = filteredMovements.stream()
                            .filter(m -> !m.getDate().isBefore(startDate) && !m.getDate().isAfter(endDate))
                            .collect(Collectors.toList());
                }
            }

            if (typeFilter.getValue() != null) {
                filteredMovements = filteredMovements.stream()
                        .filter(m -> m.getType() == typeFilter.getValue())
                        .collect(Collectors.toList());
            }

            String searchText = searchField.getText();
            if (searchText != null && !searchText.trim().isEmpty()) {
                String searchLower = searchText.toLowerCase().trim();
                filteredMovements = filteredMovements.stream()
                        .filter(m -> m.getDescription().toLowerCase().contains(searchLower))
                        .collect(Collectors.toList());
            }

            movements.setAll(filteredMovements);
            updateTotalMovementsLabel();

        } catch (Exception e) {
            showError("Errore applicazione filtri", e.getMessage());
        }
    }

    private void clearFilters() {
        searchField.clear();
        typeFilter.setValue(null);
        startDateFilter.setValue(LocalDate.now().minusMonths(1));
        endDateFilter.setValue(LocalDate.now());
        loadMovements();
    }

    // METODO PUBBLICO - chiamato dal MainController
    public void showAddNewMovementDialog() {
        try {
            Dialog<MovementDTO> dialog = createMovementDialog(null);
            Optional<MovementDTO> result = dialog.showAndWait();

            result.ifPresent(movementDTO -> {
                try {
                    System.out.println("PRIMA del service - DTO: " + movementDTO);

                    if (movementService != null) {
                        MovementDTO createdMovement = movementService.createMovement(movementDTO);
                        System.out.println("DOPO il service - DTO creato: " + createdMovement);
                        loadMovements();
                    } else {
                        movementDTO.setId(System.currentTimeMillis());
                        movements.add(movementDTO);
                        updateTotalMovementsLabel();
                    }

                    notifyBudgetUpdate();
                    showInfo("Movimento aggiunto", "Il movimento è stato aggiunto con successo.");

                } catch (Exception e) {
                    showError("Errore aggiunta movimento", e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            showError("Errore creazione dialog", e.getMessage());
        }
    }

    private void editSelectedMovement() {
        MovementDTO selectedMovement = movementsTable.getSelectionModel().getSelectedItem();
        if (selectedMovement == null) return;

        System.out.println("EDIT - Movement selezionato: " + selectedMovement);

        try {
            Dialog<MovementDTO> dialog = createMovementDialog(selectedMovement);
            Optional<MovementDTO> result = dialog.showAndWait();

            result.ifPresent(updatedMovement -> {
                System.out.println("EDIT - DTO aggiornato dal dialog: " + updatedMovement);

                try {
                    if (movementService != null) {
                        MovementDTO saved = movementService.updateMovement(selectedMovement.getId(), updatedMovement);
                        System.out.println("EDIT - DTO salvato: " + saved);
                        loadMovements();
                    } else {
                        updatedMovement.setId(selectedMovement.getId());
                        int index = movements.indexOf(selectedMovement);
                        if (index >= 0) {
                            movements.set(index, updatedMovement);
                        }
                    }

                    notifyBudgetUpdate();
                    showInfo("Movimento modificato", "Il movimento è stato modificato con successo.");
                } catch (Exception e) {
                    System.err.println("EDIT - Errore: " + e.getMessage());
                    showError("Errore modifica movimento", e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            showError("Errore creazione dialog", e.getMessage());
        }
    }

    private void deleteSelectedMovements() {
        ObservableList<MovementDTO> selectedMovements = movementsTable.getSelectionModel().getSelectedItems();
        if (selectedMovements.isEmpty()) return;

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Conferma eliminazione");
        confirmAlert.setHeaderText("Eliminazione movimenti");
        confirmAlert.setContentText(String.format("Sei sicuro di voler eliminare %d movimento/i?", selectedMovements.size()));

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                for (MovementDTO movement : selectedMovements) {
                    if (movementService != null) {
                        movementService.deleteMovement(movement.getId());
                    } else {
                        movements.remove(movement);
                    }
                }
                if (movementService != null) {
                    loadMovements();
                } else {
                    updateTotalMovementsLabel();
                }

                notifyBudgetUpdate();
                showInfo("Movimenti eliminati", "I movimenti selezionati sono stati eliminati.");
            } catch (Exception e) {
                showError("Errore eliminazione movimenti", e.getMessage());
            }
        }
    }

    private Dialog<MovementDTO> createMovementDialog(MovementDTO movement) {
        Dialog<MovementDTO> dialog = new Dialog<>();
        dialog.setTitle(movement == null ? "Nuovo Movimento" : "Modifica Movimento");
        dialog.setHeaderText(movement == null ? "Inserisci i dati del nuovo movimento" : "Modifica i dati del movimento");

        ButtonType saveButtonType = new ButtonType("Salva", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Descrizione");
        if (movement != null) {
            descriptionField.setText(movement.getDescription());
        }

        TextField amountField = new TextField();
        amountField.setPromptText("Importo");
        if (movement != null) {
            amountField.setText(movement.getAmount().toString());
        }

        ComboBox<MovementType> typeComboBox = new ComboBox<>();
        typeComboBox.setItems(FXCollections.observableArrayList(MovementType.values()));
        if (movement != null) {
            typeComboBox.setValue(movement.getType());
        } else {
            typeComboBox.setValue(MovementType.EXPENSE); // Default value
        }

        // ComboBox per le categorie - OBBLIGATORIO
        ComboBox<CategoryOption> categoryComboBox = new ComboBox<>();
        categoryComboBox.setItems(FXCollections.observableArrayList(
                new CategoryOption(1L, "Alimentari"),
                new CategoryOption(2L, "Trasporti"),
                new CategoryOption(3L, "Utenze"),
                new CategoryOption(4L, "Svago"),
                new CategoryOption(5L, "Stipendio"),
                new CategoryOption(6L, "Salute")
        ));
        categoryComboBox.setPromptText("Seleziona categoria (obbligatorio)");

        if (movement != null && movement.getCategoryIds() != null && !movement.getCategoryIds().isEmpty()) {
            Long categoryId = movement.getCategoryIds().get(0);
            categoryComboBox.getItems().stream()
                    .filter(option -> option.getId().equals(categoryId))
                    .findFirst()
                    .ifPresent(categoryComboBox::setValue);
        }

        DatePicker datePicker = new DatePicker();
        if (movement != null) {
            datePicker.setValue(movement.getDate());
        } else {
            datePicker.setValue(LocalDate.now());
        }

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Note (opzionali)");
        notesArea.setPrefRowCount(2);
        if (movement != null && movement.getNotes() != null) {
            notesArea.setText(movement.getNotes());
        }

        grid.add(new Label("Descrizione:"), 0, 0);
        grid.add(descriptionField, 1, 0);
        grid.add(new Label("Importo:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Tipo:"), 0, 2);
        grid.add(typeComboBox, 1, 2);
        grid.add(new Label("Categoria:"), 0, 3);
        grid.add(categoryComboBox, 1, 3);
        grid.add(new Label("Data:"), 0, 4);
        grid.add(datePicker, 1, 4);
        grid.add(new Label("Note:"), 0, 5);
        grid.add(notesArea, 1, 5);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // VALIDAZIONE CORRETTA - include la categoria come obbligatoria
        Runnable validateFields = () -> {
            boolean isValid = !descriptionField.getText().trim().isEmpty()
                    && !amountField.getText().trim().isEmpty()
                    && typeComboBox.getValue() != null
                    && categoryComboBox.getValue() != null; // CATEGORIA OBBLIGATORIA
            saveButton.setDisable(!isValid);
        };

        descriptionField.textProperty().addListener((observable, oldValue, newValue) -> validateFields.run());
        amountField.textProperty().addListener((observable, oldValue, newValue) -> validateFields.run());
        typeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> validateFields.run());
        categoryComboBox.valueProperty().addListener((observable, oldValue, newValue) -> validateFields.run());

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(() -> descriptionField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    MovementDTO result = new MovementDTO();
                    if (movement != null) {
                        result.setId(movement.getId());
                    }
                    result.setDescription(descriptionField.getText().trim());
                    result.setAmount(new BigDecimal(amountField.getText().trim()));
                    result.setType(typeComboBox.getValue());
                    result.setDate(datePicker.getValue());
                    result.setNotes(notesArea.getText().trim());

                    // Imposta la categoria selezionata - SEMPRE PRESENTE grazie alla validazione
                    CategoryOption selectedCategory = categoryComboBox.getValue();
                    result.setCategoryIds(List.of(selectedCategory.getId()));

                    System.out.println("DIALOG - Categoria selezionata: " + selectedCategory.getId());
                    System.out.println("DIALOG - MovementDTO creato: " + result);

                    return result;
                } catch (NumberFormatException e) {
                    showError("Errore formato importo", "L'importo inserito non è valido.");
                    return null;
                }
            }
            return null;
        });

        return dialog;
    }

    // Metodo per notificare l'aggiornamento dei budget
    private void notifyBudgetUpdate() {
        try {
            if (budgetController != null) {
                budgetController.updateBudgetWithRealMovements();
                System.out.println("Budget aggiornati automaticamente dopo modifica movimento");
            } else {
                System.out.println("BudgetController non disponibile - impossibile aggiornare budget automaticamente");
            }
            it.unicam.cs.mpgc.jbudget122631.presentation.controller.MainController main =
                    it.unicam.cs.mpgc.jbudget122631.infrastructure.config.ApplicationConfig.getMainController();
            if (main != null) {
                main.refreshAllTabsFromExternal();
                System.out.println("Richiesto refresh globale delle tab (inclusa dashboard)");
            }
        } catch (Exception e) {
            System.err.println("Errore durante notifica aggiornamento budget: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Classe helper per le opzioni delle categorie
    private static class CategoryOption {
        private final Long id;
        private final String name;

        public CategoryOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() { return id; }
        public String getName() { return name; }

        @Override
        public String toString() { return name; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CategoryOption)) return false;
            CategoryOption other = (CategoryOption) obj;
            return id.equals(other.id);
        }


        @Override
        public int hashCode() { return id.hashCode(); }
    }
}