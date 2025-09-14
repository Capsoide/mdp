package it.unicam.cs.mpgc.jbudget122631.presentation.controller;

import it.unicam.cs.mpgc.jbudget122631.application.service.*;
import it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO;
import it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType;
import it.unicam.cs.mpgc.jbudget122631.infrastructure.config.ApplicationConfig;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private BorderPane mainContainer;
    @FXML private MenuBar menuBar;
    @FXML private VBox sidePanel;
    @FXML private TabPane contentTabPane;

    // Services
    private final MovementService movementService;
    private final BudgetService budgetService;
    private final StatisticsService statisticsService;
    private final ScheduledExpenseService scheduledExpenseService;
    private final CategoryService categoryService;

    // Mappa per tenere traccia dei controller dei tab
    private final Map<String, Object> tabControllers = new HashMap<>();

    public MainController() {
        // Inizializza i servizi
        this.movementService = ApplicationConfig.getMovementService();
        this.budgetService = ApplicationConfig.getBudgetService();
        this.statisticsService = ApplicationConfig.getStatisticsService();
        this.scheduledExpenseService = ApplicationConfig.getScheduledExpenseService();
        this.categoryService = ApplicationConfig.getCategoryService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupMenuBar();
        setupTabs();
        showDashboard();
        it.unicam.cs.mpgc.jbudget122631.infrastructure.config.ApplicationConfig.setMainController(this);
    }

    private void setupMenuBar() {
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Esci");
        exitItem.setOnAction(e -> handleExit());

        Menu viewMenu = new Menu("Visualizza");
        MenuItem movementsItem = new MenuItem("Movimenti");
        MenuItem budgetsItem = new MenuItem("Budget");
        MenuItem scheduledItem = new MenuItem("Scadenzario");

        movementsItem.setOnAction(e -> showMovementsTab());
        budgetsItem.setOnAction(e -> showBudgetsTab());
        scheduledItem.setOnAction(e -> showScheduledExpensesTab());

        viewMenu.getItems().addAll(movementsItem, budgetsItem, scheduledItem);

        Menu helpMenu = new Menu("Aiuto");
        MenuItem aboutItem = new MenuItem("Informazioni");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        fileMenu.getItems().add(exitItem);
        menuBar.getMenus().addAll(fileMenu, viewMenu, helpMenu);
    }

    private void setupTabs() {
        contentTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        Tab dashboardTab = new Tab("Dashboard");
        dashboardTab.setClosable(false);
        contentTabPane.getTabs().add(dashboardTab);
    }

    private void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard-view.fxml"));
            DashboardController controller = new DashboardController(
                    movementService, budgetService, statisticsService, scheduledExpenseService);
            loader.setController(controller);  //  DEVE essere PRIMA di loader.load()

            Tab dashboardTab = contentTabPane.getTabs().get(0);
            dashboardTab.setContent(loader.load());  //  Carica DOPO aver impostato il controller
        } catch (IOException e) {
            showError("Errore caricamento dashboard", e.getMessage());
        }
    }

    private void showMovementsTab() {
        openTabIfNotExists("Movimenti", "/fxml/movements-view.fxml",
                () -> new MovementController(movementService, categoryService));
    }

    private void showBudgetsTab() {
        openTabIfNotExists("Budget", "/fxml/budgets-view.fxml",
                () -> new BudgetController(budgetService, movementService));
    }

    @FXML
    private void showScheduledExpensesTab() {
        openTabIfNotExists("Scadenzario", "/fxml/scheduled-expenses-view.fxml",
                () -> new ScheduledExpenseController(scheduledExpenseService, categoryService));
    }

    private void openTabIfNotExists(String title, String fxmlPath, ControllerSupplier supplier) {
        for (Tab tab : contentTabPane.getTabs()) {
            if (title.equals(tab.getText())) {
                contentTabPane.getSelectionModel().select(tab);
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Object controller = supplier.get();
            loader.setController(controller);

            Tab newTab = new Tab(title);
            newTab.setContent(loader.load());
            contentTabPane.getTabs().add(newTab);
            contentTabPane.getSelectionModel().select(newTab);

            // Salva il riferimento al controller
            tabControllers.put(title, controller);
        } catch (IOException e) {
            showError("Errore apertura " + title, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ControllerSupplier {
        Object get();
    }

    @FXML
    private void showNewMovementDialog() {
        // Apri il tab Movimenti se non è già aperto
        showMovementsTab();

        // Dopo un breve ritardo, invoca il dialog del MovementController
        Platform.runLater(() -> {
            MovementController movementController = (MovementController) tabControllers.get("Movimenti");
            if (movementController != null) {
                movementController.showAddNewMovementDialog();
            } else {
                showError("Errore", "Controller movimenti non disponibile");
            }
        });
    }

    // FIX: Apre direttamente il dialog per creare un nuovo budget
    @FXML
    private void showNewBudgetDialog() {
        // Apri il tab Budget se non è già aperto
        showBudgetsTab();

        // Dopo un breve ritardo, invoca il dialog del BudgetController
        Platform.runLater(() -> {
            BudgetController budgetController = (BudgetController) tabControllers.get("Budget");
            if (budgetController != null) {
                // Chiama il metodo addNewBudget() del BudgetController
                budgetController.addNewBudget();
            } else {
                showError("Errore", "Controller budget non disponibile");
            }
        });
    }

    private void refreshAllTabs() {
        // Aggiorna sempre la Dashboard
        showDashboard();

        // Aggiorna solo i tab aperti
        for (Tab tab : contentTabPane.getTabs()) {
            String tabName = tab.getText();
            switch (tabName) {
                case "Movimenti":
                    refreshMovementsTab(tab);
                    break;
                case "Budget":
                    refreshBudgetTab(tab);
                    break;
                case "Scadenzario":
                    refreshScheduledTab(tab);
                    break;
            }
        }
    }

    private void refreshMovementsTab(Tab tab) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/movements-view.fxml"));
            MovementController newController = new MovementController(movementService, categoryService);
            loader.setController(newController);
            tab.setContent(loader.load());
            tabControllers.put("Movimenti", newController);
        } catch (IOException e) {
            showError("Errore refresh movimenti", e.getMessage());
        }
    }

    private void refreshBudgetTab(Tab tab) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/budgets-view.fxml"));
            BudgetController newController = new BudgetController(budgetService, movementService);
            loader.setController(newController);
            tab.setContent(loader.load());
            tabControllers.put("Budget", newController);
        } catch (IOException e) {
            showError("Errore refresh budget", e.getMessage());
        }
    }

    private void refreshScheduledTab(Tab tab) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/scheduled-expenses-view.fxml"));
            ScheduledExpenseController newController = new ScheduledExpenseController(scheduledExpenseService, categoryService);
            loader.setController(newController);
            tab.setContent(loader.load());
            tabControllers.put("Scadenzario", newController);
        } catch (IOException e) {
            showError("Errore refresh scadenzario", e.getMessage());
        }
    }

    public void refreshAllTabsFromExternal() {
        Platform.runLater(this::refreshAllTabs);
    }

    private void handleExit() {
        ApplicationConfig.shutdown();
        System.exit(0);
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazioni");
        alert.setHeaderText("JBudget v1.0");
        alert.setContentText("Applicazione per la gestione del budget familiare\n" +
                "Sviluppata con JavaFX e Hibernate\n\n" +
                "Dashboard: Statistiche e panoramica generale\n" +
                "Movimenti: Gestione entrate e uscite\n" +
                "Budget: Pianificazione per categoria\n" +
                "Scadenzario: Spese programmate\n\n" +
                "Università di Camerino\n" +
                "Corso: Metodologie di Programmazione");
        alert.showAndWait();
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