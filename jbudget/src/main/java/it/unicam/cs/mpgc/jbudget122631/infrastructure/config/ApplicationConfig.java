package it.unicam.cs.mpgc.jbudget122631.infrastructure.config;

import it.unicam.cs.mpgc.jbudget122631.application.service.*;
import it.unicam.cs.mpgc.jbudget122631.application.service.impl.*;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.*;
import it.unicam.cs.mpgc.jbudget122631.infrastructure.persistence.*;

public final class ApplicationConfig {

    private ApplicationConfig() {}

    private static MovementRepository movementRepository;
    private static CategoryRepository categoryRepository;
    private static BudgetRepository budgetRepository;
    private static ScheduledExpenseRepository scheduledExpenseRepository;
    private static PeriodRepository periodRepository;
    private static AmortizationPlanRepository amortizationPlanRepository;
    private static it.unicam.cs.mpgc.jbudget122631.presentation.controller.MainController mainController;

    public static void setMainController(it.unicam.cs.mpgc.jbudget122631.presentation.controller.MainController mc) {
        mainController = mc;
    }

    public static it.unicam.cs.mpgc.jbudget122631.presentation.controller.MainController getMainController() {
        return mainController;
    }

    public static MovementRepository getMovementRepository() {
        if (movementRepository == null) movementRepository = new JpaMovementRepository();
        return movementRepository;
    }

    public static CategoryRepository getCategoryRepository() {
        if (categoryRepository == null) categoryRepository = new JpaCategoryRepository();
        return categoryRepository;
    }

    public static BudgetRepository getBudgetRepository() {
        if (budgetRepository == null) budgetRepository = new JpaBudgetRepository();
        return budgetRepository;
    }

    public static ScheduledExpenseRepository getScheduledExpenseRepository() {
        if (scheduledExpenseRepository == null) scheduledExpenseRepository = new JpaScheduledExpenseRepository();
        return scheduledExpenseRepository;
    }

    public static PeriodRepository getPeriodRepository() {
        if (periodRepository == null) periodRepository = new JpaPeriodRepository();
        return periodRepository;
    }

    public static AmortizationPlanRepository getAmortizationPlanRepository() {
        if (amortizationPlanRepository == null) amortizationPlanRepository = new JpaAmortizationPlanRepository();
        return amortizationPlanRepository;
    }

    private static MovementService movementService;
    private static CategoryService categoryService;
    private static BudgetService budgetService;
    private static ScheduledExpenseService scheduledExpenseService;
    private static StatisticsService statisticsService;

    public static BudgetService getBudgetService() {
        if (budgetService == null) {
            budgetService = new BudgetServiceImpl(
                    getBudgetRepository(),
                    getMovementRepository(),
                    getPeriodRepository(),
                    getCategoryRepository()
            );
            System.out.println("INIT - BudgetService inizializzato");
        }
        return budgetService;
    }

    public static MovementService getMovementService() {
        if (movementService == null) {
            // Prima inizializza il BudgetService
            BudgetService budgetSvc = getBudgetService();

            // Poi crea il MovementService con il BudgetService per aggiornamento automatico
            movementService = new MovementServiceImpl(
                    getMovementRepository(),
                    getCategoryRepository(),
                    getPeriodRepository(),
                    budgetSvc  // Passa il BudgetService per aggiornamento automatico
            );

            System.out.println("INIT - MovementService inizializzato con aggiornamento budget automatico");
        }
        return movementService;
    }

    public static CategoryService getCategoryService() {
        if (categoryService == null) {
            categoryService = new CategoryServiceImpl(getCategoryRepository());
            System.out.println("INIT - CategoryService inizializzato");
        }
        return categoryService;
    }

    public static ScheduledExpenseService getScheduledExpenseService() {
        if (scheduledExpenseService == null) {
            scheduledExpenseService = new ScheduledExpenseServiceImpl(
                    getScheduledExpenseRepository(),
                    getMovementService()
            );
            System.out.println("INIT - ScheduledExpenseService inizializzato");
        }
        return scheduledExpenseService;
    }

    public static StatisticsService getStatisticsService() {
        if (statisticsService == null) {
            statisticsService = new StatisticsServiceImpl(
                    getMovementRepository(),
                    getBudgetRepository(),
                    getCategoryRepository()
            );
            System.out.println("INIT - StatisticsService inizializzato");
        }
        return statisticsService;
    }

    public static void initializeServices() {
        System.out.println("INIT - Avvio inizializzazione servizi...");

        getCategoryService();
        getBudgetService();
        getMovementService();
        getScheduledExpenseService();
        getStatisticsService();

        System.out.println("INIT - Tutti i servizi inizializzati con successo!");
        System.out.println("INIT - Aggiornamento automatico budget ABILITATO");
    }

    public static void testBudgetIntegration() {
        System.out.println("TEST - Verificando integrazione Budget-Movement...");

        try {
            MovementService mvService = getMovementService();
            BudgetService bgService = getBudgetService();

            if (mvService != null && bgService != null) {
                System.out.println("TEST - ✓ MovementService disponibile");
                System.out.println("TEST - ✓ BudgetService disponibile");
                System.out.println("TEST - ✓ Aggiornamento automatico PRONTO");


                if (mvService instanceof MovementServiceImpl) {
                    System.out.println("TEST - ✓ MovementServiceImpl con aggiornamento budget configurato");
                }
            } else {
                System.out.println("TEST - ✗ Errore: Servizi non disponibili");
            }

        } catch (Exception e) {
            System.err.println("TEST - ✗ Errore test integrazione: " + e.getMessage());
        }
    }

    public static void shutdown() {
        try {
            System.out.println("SHUTDOWN - Chiusura servizi...");

            // Reset dei servizi
            movementService = null;
            budgetService = null;
            categoryService = null;
            scheduledExpenseService = null;
            statisticsService = null;

            // Reset dei repository
            movementRepository = null;
            budgetRepository = null;
            categoryRepository = null;
            scheduledExpenseRepository = null;
            periodRepository = null;
            amortizationPlanRepository = null;

            // Shutdown di Hibernate
            HibernateConfig.shutdown();

            System.out.println("SHUTDOWN - Completato");
        } catch (Exception e) {
            System.err.println("SHUTDOWN - Errore: " + e.getMessage());
        }
    }
}