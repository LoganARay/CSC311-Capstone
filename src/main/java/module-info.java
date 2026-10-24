module edu.farmingdale.csc311capstone {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.farmingdale.csc311capstone to javafx.fxml;
    opens edu.farmingdale.csc311capstone.Pacman to javafx.fxml;
    opens edu.farmingdale.csc311capstone.Pong to javafx.fxml;
    opens edu.farmingdale.csc311capstone.Breakout to javafx.fxml;
    exports edu.farmingdale.csc311capstone;
}