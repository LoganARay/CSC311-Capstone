package edu.farmingdale.csc311capstone.dashboard;

import edu.farmingdale.csc311capstone.SceneSwitcher;

import javafx.fxml.FXML;

public final class RegistrationController extends AuthController {
    @FXML
    private void login() {
        clearSecrets();
        SceneSwitcher.switchTo(root, "dashboard/login-view.fxml", "Arcade · Log in");
    }
}
