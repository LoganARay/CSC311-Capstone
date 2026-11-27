package edu.farmingdale.csc311capstone.dashboard;

import edu.farmingdale.csc311capstone.SceneSwitcher;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/** Forms are usable previews, with no credentials stored or fake authentication success. */
public abstract class AuthController {
    @FXML protected ScrollPane root;
    @FXML private HBox layout, brand;
    @FXML private VBox copy, formArea;
    @FXML private StackPane artwork;
    @FXML protected TextField username;
    @FXML protected PasswordField password, confirm;
    @FXML protected TextField visiblePassword, visibleConfirm;
    @FXML protected Label feedback;
    @FXML private Button showPassword, showConfirm, submitButton;
    @FXML private ProgressIndicator loading;

    @FXML
    private void initialize() {
        root.setUserData(this);
        Label logo = Ui.label("", "brand-icon");
        logo.setGraphic(Icons.of("game"));
        brand.getChildren().setAll(logo, Ui.label("arcade", "brand-name"));
        artwork.getChildren().add(new ArcadeArt("cabinet"));
        configureVisibility(password, visiblePassword, showPassword);
        if (confirm != null) configureVisibility(confirm, visibleConfirm, showConfirm);
        root.viewportBoundsProperty().addListener((o, a, b) -> layout.setMinHeight(b.getHeight()));
        layout.widthProperty()
                .addListener(
                        (o, a, b) -> {
                            double w = b.doubleValue();
                            double left = Math.floor(w * (w < 1100 ? .38 : .5));
                            copy.setPrefWidth(left);
                            copy.setMinWidth(0);
                            copy.setMaxWidth(left);
                            formArea.setPrefWidth(w - left);
                            formArea.setMinWidth(0);
                            formArea.setMaxWidth(w - left);
                        });
    }

    private void configureVisibility(PasswordField hidden, TextField visible, Button button) {
        visible.textProperty().bindBidirectional(hidden.textProperty());
        button.setOnAction(
                e -> {
                    boolean show = !visible.isVisible();
                    visible.setVisible(show);
                    visible.setManaged(show);
                    hidden.setVisible(!show);
                    hidden.setManaged(!show);
                    button.setText(show ? "Hide" : "Show");
                    button.setAccessibleText(show ? "Hide password" : "Show password");
                    if (show) visible.requestFocus();
                    else hidden.requestFocus();
                });
    }

    /** Backend integration can use this while an actual request is pending. */
    public void setBusy(boolean busy) {
        submitButton.setDisable(busy);
        loading.setVisible(busy);
        loading.setManaged(busy);
        username.setDisable(busy);
        password.setDisable(busy);
        visiblePassword.setDisable(busy);
        if (confirm != null) {
            confirm.setDisable(busy);
            visibleConfirm.setDisable(busy);
        }
    }

    public void showAuthenticationError(String message) {
        setBusy(false);
        feedback.getStyleClass().remove("error");
        feedback.getStyleClass().add("error");
        feedback.setText(message);
    }

    @FXML
    protected void submit() {
        List<String> errors =
                confirm == null
                        ? FormValidation.login(username.getText(), password.getText())
                        : FormValidation.registration(
                                username.getText(), password.getText(), confirm.getText());
        feedback.getStyleClass().remove("error");
        if (!errors.isEmpty()) {
            feedback.getStyleClass().add("error");
            feedback.setText(String.join("\n", errors));
            return;
        }
        // No backend contract exists. Do not send, persist, or accept credentials.
        password.clear();
        if (confirm != null) confirm.clear();
        feedback.setText(
                "Account services are not connected yet. Continue as a guest to explore the"
                    + " arcade.");
    }

    @FXML
    protected void guest() {
        clearSecrets();
        SceneSwitcher.showMenu(root);
    }

    protected void clearSecrets() {
        password.clear();
        if (confirm != null) confirm.clear();
    }
}
