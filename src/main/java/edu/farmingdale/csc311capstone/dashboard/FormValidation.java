package edu.farmingdale.csc311capstone.dashboard;

import java.util.ArrayList;
import java.util.List;

/** Required-field checks only. Password policy belongs to the team's authentication backend. */
public final class FormValidation {
    private FormValidation() {}

    public static List<String> login(String username, String password) {
        List<String> errors = new ArrayList<>();
        if (username == null || username.isBlank()) errors.add("Enter your username.");
        if (password == null || password.isBlank()) errors.add("Enter your password.");
        return List.copyOf(errors);
    }

    public static List<String> registration(String username, String password, String confirmation) {
        List<String> errors = new ArrayList<>(login(username, password));
        if (confirmation == null || confirmation.isBlank()) errors.add("Confirm your password.");
        else if (!confirmation.equals(password)) errors.add("Your passwords do not match.");
        return List.copyOf(errors);
    }
}
