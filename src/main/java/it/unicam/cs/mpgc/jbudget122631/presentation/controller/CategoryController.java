package it.unicam.cs.mpgc.jbudget122631.presentation.controller;

import it.unicam.cs.mpgc.jbudget122631.application.service.CategoryService;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CategoryController implements Initializable {

    @FXML private TreeView<Category> categoriesTreeView;
    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, String> nameColumn;
    @FXML private TableColumn<Category, String> descriptionColumn;
    @FXML private TableColumn<Category, String> parentColumn;
    @FXML private TableColumn<Category, String> pathColumn;
    @FXML private TableColumn<Category, String> statusColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button searchButton;
    @FXML private Button clearFilterButton;
    @FXML private Button addCategoryButton;
    @FXML private Button editCategoryButton;
    @FXML private Button deleteCategoryButton;
    @FXML private Button activateButton;
    @FXML private Button deactivateButton;

    @FXML private TextField newCategoryNameField;
    @FXML private TextField newCategoryDescriptionField;
    @FXML private ComboBox<Category> parentCategoryCombo;
    @FXML private Button createButton;

    private final CategoryService categoryService;
    private final ObservableList<Category> categories = FXCollections.observableArrayList();

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Costruttore di default per FXML
    public CategoryController() {
        this.categoryService = null;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupTreeView();
        setupFilters();
        setupButtons();
        setupCreationForm();
        loadCategories();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        parentColumn.setCellValueFactory(cellData -> {
            Category parent = cellData.getValue().getParent();
            return new SimpleStringProperty(parent != null ? parent.getName() : "Root");
        });

        pathColumn.setCellValueFactory(cellData -> {
            String path = cellData.getValue().getPath().stream()
                    .map(Category::getName)
                    .collect(Collectors.joining(" > "));
            return new SimpleStringProperty(path);
        });

        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().isActive() ? "Attiva" : "Disattivata";
            return new SimpleStringProperty(status);
        });

        // Formattazione status column
        statusColumn.setCellFactory(column -> new TableCell<Category, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Attiva".equals(status)) {
                        setStyle("-fx-text-fill: green;");
                    } else {
                        setStyle("-fx-text-fill: red;");
                    }
                }
            }
        });

        categoriesTable.setItems(categories);
        categoriesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Modifica");
        MenuItem deleteItem = new MenuItem("Elimina");
        MenuItem activateItem = new MenuItem("Attiva");
        MenuItem deactivateItem = new MenuItem("Disattiva");

        editItem.setOnAction(e -> editSelectedCategory());
        deleteItem.setOnAction(e -> deleteSelectedCategory());
        activateItem.setOnAction(e -> activateSelectedCategory());
        deactivateItem.setOnAction(e -> deactivateSelectedCategory());

        contextMenu.getItems().addAll(editItem, deleteItem, new SeparatorMenuItem(),
                activateItem, deactivateItem);
        categoriesTable.setContextMenu(contextMenu);
    }

    private void setupTreeView() {
        // Configurazione TreeView per visualizzazione gerarchica
        categoriesTreeView.setShowRoot(false);
        categoriesTreeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Cell factory per visualizzazione custom
        categoriesTreeView.setCellFactory(tree -> new TreeCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(category.getName());
                    if (!category.isActive()) {
                        setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(
                "Tutte", "Solo attive", "Solo disattivate"
        ));
        statusFilter.setValue("Tutte");

        searchButton.setOnAction(e -> applyFilters());
        clearFilterButton.setOnAction(e -> clearFilters());
    }

    private void setupButtons() {
        addCategoryButton.setOnAction(e -> showCreateCategoryForm());
        editCategoryButton.setOnAction(e -> editSelectedCategory());
        deleteCategoryButton.setOnAction(e -> deleteSelectedCategory());
        activateButton.setOnAction(e -> activateSelectedCategory());
        deactivateButton.setOnAction(e -> deactivateSelectedCategory());

        // Disabilita bottoni se nessuna selezione
        editCategoryButton.disableProperty().bind(
                categoriesTable.getSelectionModel().selectedItemProperty().isNull());
        deleteCategoryButton.disableProperty().bind(
                categoriesTable.getSelectionModel().selectedItemProperty().isNull());
        activateButton.disableProperty().bind(
                categoriesTable.getSelectionModel().selectedItemProperty().isNull());
        deactivateButton.disableProperty().bind(
                categoriesTable.getSelectionModel().selectedItemProperty().isNull());
    }

    private void setupCreationForm() {
        createButton.setOnAction(e -> createNewCategory());
        loadParentCategories();
    }

    private void loadCategories() {
        try {
            if (categoryService != null) {
                List<Category> allCategories = categoryService.getAllCategories();
                categories.setAll(allCategories);
                updateTreeView(allCategories);
            } else {
                loadSampleCategories();
            }
        } catch (Exception e) {
            showError("Errore caricamento categorie", e.getMessage());
            loadSampleCategories();
        }
    }

    private void loadSampleCategories() {
        // Dati di esempio per test
        Category root1 = new Category("Casa");
        Category child1 = new Category("Utenze");
        Category child2 = new Category("Manutenzione");
        root1.addChild(child1);
        root1.addChild(child2);

        Category root2 = new Category("Trasporti");
        Category child3 = new Category("Benzina");
        root2.addChild(child3);

        categories.setAll(root1, child1, child2, root2, child3);
        updateTreeView(categories);
    }

    private void updateTreeView(List<Category> categoryList) {
        if (categoriesTreeView == null) return;

        TreeItem<Category> root = new TreeItem<>();

        // Trova le categorie root
        List<Category> rootCategories = categoryList.stream()
                .filter(cat -> cat.getParent() == null)
                .collect(Collectors.toList());

        for (Category rootCategory : rootCategories) {
            TreeItem<Category> rootItem = new TreeItem<>(rootCategory);
            addChildrenToTreeItem(rootItem, rootCategory);
            root.getChildren().add(rootItem);
        }

        categoriesTreeView.setRoot(root);

        // Espandi tutti i nodi
        expandTreeView(root);
    }

    private void addChildrenToTreeItem(TreeItem<Category> parentItem, Category parentCategory) {
        for (Category child : parentCategory.getChildren()) {
            TreeItem<Category> childItem = new TreeItem<>(child);
            addChildrenToTreeItem(childItem, child);
            parentItem.getChildren().add(childItem);
        }
    }

    private void expandTreeView(TreeItem<Category> item) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(true);
            for (TreeItem<Category> child : item.getChildren()) {
                expandTreeView(child);
            }
        }
    }

    private void loadParentCategories() {
        if (parentCategoryCombo == null) return;

        parentCategoryCombo.getItems().clear();
        parentCategoryCombo.getItems().add(null); // Opzione "Root"

        if (categoryService != null) {
            try {
                List<Category> activeCategories = categoryService.getActiveCategories();
                parentCategoryCombo.getItems().addAll(activeCategories);
            } catch (Exception e) {
                System.err.println("Errore caricamento categorie parent: " + e.getMessage());
            }
        }

        // Custom cell factory per visualizzare "Root" per null
        parentCategoryCombo.setCellFactory(listView -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setText(empty ? null : (category == null ? "Root" : category.getName()));
            }
        });

        parentCategoryCombo.setButtonCell(new ListCell<Category>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setText(empty ? null : (category == null ? "Root" : category.getName()));
            }
        });
    }

    private void applyFilters() {
        // Implementazione filtri semplificata
        loadCategories();
    }

    private void clearFilters() {
        searchField.clear();
        statusFilter.setValue("Tutte");
        loadCategories();
    }

    private void showCreateCategoryForm() {
        // Focus sul form di creazione
        if (newCategoryNameField != null) {
            newCategoryNameField.requestFocus();
        }
    }

    private void createNewCategory() {
        if (newCategoryNameField == null || categoryService == null) return;

        String name = newCategoryNameField.getText().trim();
        String description = newCategoryDescriptionField != null ?
                newCategoryDescriptionField.getText().trim() : "";
        Category parent = parentCategoryCombo != null ?
                parentCategoryCombo.getValue() : null;

        if (name.isEmpty()) {
            showError("Nome richiesto", "Il nome della categoria è obbligatorio");
            return;
        }

        try {
            Long parentId = parent != null ? parent.getId() : null;
            Category newCategory = categoryService.createCategory(name, description, parentId);

            // Reset form
            newCategoryNameField.clear();
            if (newCategoryDescriptionField != null) newCategoryDescriptionField.clear();
            if (parentCategoryCombo != null) parentCategoryCombo.setValue(null);

            // Ricarica lista
            loadCategories();
            loadParentCategories();

            showInfo("Categoria creata", "Categoria '" + name + "' creata con successo");

        } catch (Exception e) {
            showError("Errore creazione categoria", e.getMessage());
        }
    }

    private void editSelectedCategory() {
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showInfo("Modifica Categoria", "Dialog per modifica categoria non ancora implementato");
        }
    }

    private void deleteSelectedCategory() {
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (categoryService != null && categoryService.canDeleteCategory(selected.getId())) {
            showError("Impossibile eliminare",
                    "La categoria ha delle sottocategorie o movimenti associati");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Conferma eliminazione");
        confirmation.setHeaderText("Eliminare la categoria '" + selected.getName() + "'?");
        confirmation.setContentText("Questa operazione non può essere annullata.");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                if (categoryService != null) {
                    categoryService.deleteCategory(selected.getId());
                }
                loadCategories();
                loadParentCategories();
                showInfo("Eliminazione completata", "Categoria eliminata con successo");
            } catch (Exception e) {
                showError("Errore eliminazione", e.getMessage());
            }
        }
    }

    private void activateSelectedCategory() {
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected != null && categoryService != null) {
            try {
                categoryService.activateCategory(selected.getId());
                loadCategories();
                showInfo("Categoria attivata", "Categoria attivata con successo");
            } catch (Exception e) {
                showError("Errore attivazione", e.getMessage());
            }
        }
    }

    private void deactivateSelectedCategory() {
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected != null && categoryService != null) {
            try {
                categoryService.deactivateCategory(selected.getId());
                loadCategories();
                showInfo("Categoria disattivata", "Categoria disattivata con successo");
            } catch (Exception e) {
                showError("Errore disattivazione", e.getMessage());
            }
        }
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