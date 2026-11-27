package edu.farmingdale.csc311capstone.dashboard;

import java.time.Duration;
import java.util.Locale;

public final class DisplayFormats {
    private DisplayFormats() {}

    public static String number(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    public static String duration(Duration duration) {
        if (duration == null) return "—";
        long minutes = Math.max(0, duration.toMinutes());
        return minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
    }

    public static String outcome(String value) {
        return value == null || value.isBlank() ? "Completed" : value;
    }
}
