package it.unicam.cs.mpgc.jbudget122631.presentation.controller;

import it.unicam.cs.mpgc.jbudget122631.application.dto.StatisticsDTO;
import it.unicam.cs.mpgc.jbudget122631.application.service.*;
import it.unicam.cs.mpgc.jbudget122631.domain.model.ScheduledExpense;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label balanceLabel;
    @FXML private Label overdueExpensesLabel;
    @FXML private ComboBox<String> periodSelectorCombo;
    @FXML private Button refreshButton;

    @FXML private PieChart expensesByCategoryChart;
    @FXML private LineChart<String, Number> monthlyTrendChart;
    @FXML private VBox upcomingExpensesBox;
    @FXML private VBox recentMovementsBox;

    private MovementService movementService;
    private BudgetService budgetService;
    private StatisticsService statisticsService;
    private ScheduledExpenseService scheduledExpenseService;

    private enum PeriodType {
        CURRENT_MONTH("Mese Corrente"),
        LAST_3_MONTHS("Ultimi 3 Mesi"),
        LAST_6_MONTHS("Ultimi 6 Mesi"),
        CURRENT_YEAR("Anno Corrente");

        private final String displayName;

        PeriodType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public DashboardController(MovementService movementService, BudgetService budgetService,
                               StatisticsService statisticsService, ScheduledExpenseService scheduledExpenseService) {
        this.movementService = movementService;
        this.budgetService = budgetService;
        this.statisticsService = statisticsService;
        this.scheduledExpenseService = scheduledExpenseService;

        System.out.println("DashboardController inizializzato con servizi reali!");
        System.out.println("MovementService: " + (movementService != null ? "OK" : "NULL"));
        System.out.println("StatisticsService: " + (statisticsService != null ? "OK" : "NULL"));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("initialize() chiamato - caricamento dashboard...");
        setupPeriodSelector();
        loadDashboardData();
    }

    private void setupPeriodSelector() {
        if (periodSelectorCombo != null) {
            periodSelectorCombo.setItems(FXCollections.observableArrayList(
                    PeriodType.CURRENT_MONTH.getDisplayName(),
                    PeriodType.LAST_3_MONTHS.getDisplayName(),
                    PeriodType.LAST_6_MONTHS.getDisplayName(),
                    PeriodType.CURRENT_YEAR.getDisplayName()
            ));

            periodSelectorCombo.setValue(PeriodType.LAST_6_MONTHS.getDisplayName());

            periodSelectorCombo.setOnAction(e -> {
                loadDashboardData();
                Platform.runLater(() -> forceChartLayout());
            });
        }

        if (refreshButton != null) {
            refreshButton.setOnAction(e -> refreshDashboard());
        }
    }

    private void loadDashboardData() {
        try {
            PeriodDates dates = getSelectedPeriodDates();

            if (statisticsService != null) {
                System.out.println("Caricamento dati reali dal servizio per periodo: " +
                        dates.startDate + " - " + dates.endDate);
                StatisticsDTO stats = statisticsService.getStatisticsForPeriod(dates.startDate, dates.endDate);

                updateSummaryLabels(stats);
                updateExpensesByCategoryChart(stats);
                updateMonthlyTrendChart(dates.startDate, dates.endDate);
            } else {
                System.out.println("Servizi non disponibili");
            }

            updateUpcomingExpenses();
            updateRecentMovements();

        } catch (Exception e) {
            System.err.println("Errore caricamento dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private PeriodDates getSelectedPeriodDates() {
        String selectedPeriod = periodSelectorCombo != null ?
                periodSelectorCombo.getValue() :
                PeriodType.LAST_6_MONTHS.getDisplayName();

        LocalDate now = LocalDate.now();
        LocalDate startDate, endDate;

        if (PeriodType.CURRENT_MONTH.getDisplayName().equals(selectedPeriod)) {
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        } else if (PeriodType.LAST_3_MONTHS.getDisplayName().equals(selectedPeriod)) {
            endDate = now.withDayOfMonth(now.lengthOfMonth());
            startDate = endDate.minusMonths(2).withDayOfMonth(1);
        } else if (PeriodType.LAST_6_MONTHS.getDisplayName().equals(selectedPeriod)) {
            endDate = now.withDayOfMonth(now.lengthOfMonth());
            startDate = endDate.minusMonths(5).withDayOfMonth(1);
        } else if (PeriodType.CURRENT_YEAR.getDisplayName().equals(selectedPeriod)) {
            startDate = LocalDate.of(now.getYear(), 1, 1);
            endDate = LocalDate.of(now.getYear(), 12, 31);
        } else {
            endDate = now.withDayOfMonth(now.lengthOfMonth());
            startDate = endDate.minusMonths(5).withDayOfMonth(1);
        }

        return new PeriodDates(startDate, endDate);
    }

    private static class PeriodDates {
        final LocalDate startDate;
        final LocalDate endDate;

        PeriodDates(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }



    private void updateSummaryLabels(StatisticsDTO stats) {
        if (totalIncomeLabel != null) totalIncomeLabel.setText(String.format("\u20AC %.2f", stats.getTotalIncome()));
        if (totalExpensesLabel != null) totalExpensesLabel.setText(String.format("\u20AC %.2f", stats.getTotalExpenses()));

        BigDecimal balance = stats.getBalance();
        if (balanceLabel != null) {
            balanceLabel.setText(String.format("\u20AC %.2f", balance));

            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                balanceLabel.setStyle("-fx-text-fill: green; -fx-font-size: 24px; -fx-font-weight: normal;");
            } else {
                balanceLabel.setStyle("-fx-text-fill: red; -fx-font-size: 24px; -fx-font-weight: normal;");
            }
        }

        if (scheduledExpenseService != null && overdueExpensesLabel != null) {
            List<ScheduledExpense> overdueExpenses = scheduledExpenseService.getOverdueExpenses();
            overdueExpensesLabel.setText(String.valueOf(overdueExpenses.size()));
            if (!overdueExpenses.isEmpty()) {
                overdueExpensesLabel.setStyle("-fx-text-fill: red; -fx-font-weight: normal; -fx-font-size: 24px;");
            }
        }
    }

    private void updateExpensesByCategoryChart(StatisticsDTO stats) {
        if (expensesByCategoryChart == null) return;

        Map<String, BigDecimal> expensesByCategory = stats.getExpensesByCategory();

        if (expensesByCategory != null && !expensesByCategory.isEmpty()) {
            expensesByCategoryChart.setData(FXCollections.observableArrayList(
                    expensesByCategory.entrySet().stream()
                            .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue().doubleValue()))
                            .toArray(PieChart.Data[]::new)
            ));
        } else {
            expensesByCategoryChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Nessun dato", 1)
            ));
        }
    }

    private void updateMonthlyTrendChart(LocalDate startDate, LocalDate endDate) {
        if (monthlyTrendChart == null || statisticsService == null) return;

        try {
            //System.out.println("TREND - Caricamento trend da " + startDate + " a " + endDate);

            Map<String, BigDecimal> monthlyTrend = statisticsService.getMonthlyIncomeExpensesTrend(startDate, endDate);

            //System.out.println("TREND - Dati ricevuti: " + monthlyTrend.size() + " mesi");
            monthlyTrend.forEach((k, v) -> System.out.println("  " + k + " = €" + v));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Bilancio Mensile");

            Map<String, BigDecimal> completeMonthlyData = generateCompleteMonthlyData(startDate, endDate, monthlyTrend);

            completeMonthlyData.entrySet().forEach(entry -> {
                //System.out.println("TREND - Aggiunta data point: " + entry.getKey() + " = " + entry.getValue());
                String displayDate = formatMonthForDisplay(entry.getKey());
                series.getData().add(new XYChart.Data<>(displayDate, entry.getValue().doubleValue()));
            });

            monthlyTrendChart.getData().clear();
            monthlyTrendChart.getData().add(series);

            if (monthlyTrendChart.getXAxis() instanceof javafx.scene.chart.CategoryAxis) {
                javafx.scene.chart.CategoryAxis xAxis = (javafx.scene.chart.CategoryAxis) monthlyTrendChart.getXAxis();
                xAxis.setAutoRanging(false);
                xAxis.setAutoRanging(true);
                xAxis.setGapStartAndEnd(true);
            }

            Platform.runLater(() -> {
                forceChartLayout();
            });

            //System.out.println("TREND - Grafico aggiornato con " + series.getData().size() + " punti");

        } catch (Exception e) {
            System.err.println("Errore aggiornamento trend mensile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Map<String, BigDecimal> generateCompleteMonthlyData(LocalDate startDate, LocalDate endDate,
                                                                Map<String, BigDecimal> actualData) {
        Map<String, BigDecimal> completeData = new java.util.LinkedHashMap<>();

        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);

        YearMonth current = start;
        while (!current.isAfter(end)) {
            String monthKey = current.toString();
            BigDecimal value = actualData.getOrDefault(monthKey, BigDecimal.ZERO);
            completeData.put(monthKey, value);
            current = current.plusMonths(1);
        }

        return completeData;
    }

    private String formatMonthForDisplay(String yearMonth) {
        try {
            YearMonth ym = YearMonth.parse(yearMonth);
            return ym.getMonth().getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    java.util.Locale.ITALIAN
            ) + " " + String.valueOf(ym.getYear()).substring(2);
        } catch (Exception e) {
            return yearMonth;
        }
    }
    private void forceChartLayout() {
        if (monthlyTrendChart != null) {
            monthlyTrendChart.layout();
            monthlyTrendChart.autosize();
            monthlyTrendChart.getXAxis().setAutoRanging(false);
            monthlyTrendChart.getXAxis().setAutoRanging(true);
        }
        if (expensesByCategoryChart != null) {
            expensesByCategoryChart.layout();
            expensesByCategoryChart.autosize();
        }
    }

    private void updateUpcomingExpenses() {
        if (upcomingExpensesBox == null) return;

        try {
            if (scheduledExpenseService != null) {
                List<ScheduledExpense> upcomingExpenses = scheduledExpenseService.getAllScheduledExpenses()
                        .stream()
                        .filter(expense -> !expense.isCompleted())
                        .filter(expense -> expense.getDaysUntilDue() >= 0 && expense.getDaysUntilDue() <= 7)
                        .collect(Collectors.toList());

                upcomingExpensesBox.getChildren().clear();

                if (upcomingExpenses.isEmpty()) {
                    upcomingExpensesBox.getChildren().add(new Label("Nessuna spesa in scadenza"));
                } else {
                    upcomingExpenses.stream()
                            .limit(5)
                            .forEach(expense -> {
                                Label expenseLabel = new Label(String.format("%s - \u20AC%.2f (%s)",
                                        expense.getDescription(), expense.getAmount(), expense.getDueDate()));

                                if (expense.isOverdue()) {
                                    expenseLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 12px;");
                                } else if (expense.isDue()) {
                                    expenseLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 12px;");
                                }

                                upcomingExpensesBox.getChildren().add(expenseLabel);
                            });
                }
            }

        } catch (Exception e) {
            System.err.println("Errore aggiornamento spese in scadenza: " + e.getMessage());
        }
    }

    private void updateRecentMovements() {
        if (recentMovementsBox == null) return;

        try {
            if (movementService != null) {
                List<it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO> recentMovements = null;

                try {
                    List<it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO> allMovements =
                            movementService.getAllMovements();

                    if (allMovements != null && !allMovements.isEmpty()) {
                        recentMovements = allMovements.stream()
                                .sorted((m1, m2) -> {
                                    int dateComparison = m2.getDate().compareTo(m1.getDate());
                                    if (dateComparison == 0) {
                                        return m2.getId().compareTo(m1.getId());
                                    }
                                    return dateComparison;
                                })
                                .limit(5)
                                .collect(java.util.stream.Collectors.toList());
                    }
                } catch (Exception e) {
                    System.out.println("Errore nel caricamento movimenti: " + e.getMessage());
                    e.printStackTrace();
                }

                recentMovementsBox.getChildren().clear();

                if (recentMovements == null || recentMovements.isEmpty()) {
                    recentMovementsBox.getChildren().add(new Label("Nessun movimento recente"));
                } else {
                    for (int i = 0; i < recentMovements.size(); i++) {
                        it.unicam.cs.mpgc.jbudget122631.application.dto.MovementDTO movement = recentMovements.get(i);

                        Label movementLabel = new Label(String.format("%s - \u20AC%.2f (%s)",
                                movement.getDescription(),
                                movement.getAmount(),
                                movement.getDate()));

                        if (movement.getType() == it.unicam.cs.mpgc.jbudget122631.domain.model.MovementType.INCOME) {
                            movementLabel.setStyle("-fx-text-fill: green;");
                        } else {
                            movementLabel.setStyle("-fx-text-fill: red;");
                        }

                        recentMovementsBox.getChildren().add(movementLabel);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Errore aggiornamento movimenti recenti: " + e.getMessage());
            e.printStackTrace();
            recentMovementsBox.getChildren().clear();
            recentMovementsBox.getChildren().add(new Label("Errore caricamento movimenti"));
        }
    }

    public void refreshDashboard() {
        loadDashboardData();

        Platform.runLater(() -> {
            forceChartLayout();
            Platform.runLater(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
                forceChartLayout();
            });
        });
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    public MovementService getMovementService() {
        return movementService;
    }
}