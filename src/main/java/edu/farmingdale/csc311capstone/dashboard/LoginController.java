package edu.farmingdale.csc311capstone.dashboard;

import edu.farmingdale.csc311capstone.SceneSwitcher;

import javafx.fxml.FXML;

public final class LoginController extends AuthController {
    @FXML
    private void register() {
        clearSecrets();
        SceneSwitcher.switchTo(root, "dashboard/registration-view.fxml", "Arcade · Create account");
    }
}
