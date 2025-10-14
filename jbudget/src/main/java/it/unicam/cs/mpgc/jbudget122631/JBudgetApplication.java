package it.unicam.cs.mpgc.jbudget122631;

import it.unicam.cs.mpgc.jbudget122631.infrastructure.config.ApplicationConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class JBudgetApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    JBudgetApplication.class.getResource("/fxml/main-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 1200, 800);
            primaryStage.setTitle("JBudget - Gestione Budget Familiare");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setOnCloseRequest(e -> ApplicationConfig.shutdown());
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            ApplicationConfig.shutdown();
            throw new RuntimeException("Errore durante l'avvio della GUI", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
