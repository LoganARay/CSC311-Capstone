package edu.farmingdale.csc311capstone;

import edu.farmingdale.csc311capstone.dashboard.DashboardPreferences;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Scene scene =
                new Scene(FXMLLoader.load(HelloApplication.class.getResource("hello-view.fxml")));
        stage.setTitle(SceneSwitcher.MENU_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        var values = new DashboardPreferences().current();
        SceneSwitcher.applyDashboardSize(stage, values.size());
        stage.setMaximized(values.startMaximized());
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
