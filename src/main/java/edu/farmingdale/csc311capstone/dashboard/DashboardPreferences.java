package edu.farmingdale.csc311capstone.dashboard;

import java.util.prefs.*;

/** Local, non-sensitive dashboard preferences. Never used by individual game audio or accounts. */
public final class DashboardPreferences {
    public record Values(
            boolean reduceMotion, boolean animations, boolean startMaximized, String size) {}

    public static final Values DEFAULTS = new Values(false, true, false, "1440 × 900");
    public static final java.util.List<String> SIZES =
            java.util.List.of("1440 × 900", "1280 × 800", "1024 × 768");
    private final Preferences store;
    private Values current;

    public DashboardPreferences() {
        this(Preferences.userRoot().node("edu/farmingdale/csc311capstone/dashboard"));
    }

    public DashboardPreferences(Preferences store) {
        this.store = store;
        current = load();
    }

    public Values current() {
        return current;
    }

    public Values load() {
        String size = store.get("size", DEFAULTS.size());
        return new Values(
                store.getBoolean("reduceMotion", DEFAULTS.reduceMotion()),
                store.getBoolean("animations", DEFAULTS.animations()),
                store.getBoolean("startMaximized", DEFAULTS.startMaximized()),
                SIZES.contains(size) ? size : DEFAULTS.size());
    }

    public void save(Values values) throws BackingStoreException {
        if (!SIZES.contains(values.size()))
            throw new IllegalArgumentException("Unsupported window size");
        Values previous = current;
        try {
            write(values);
            store.flush();
            current = values;
        } catch (BackingStoreException | SecurityException failure) {
            write(previous);
            throw failure;
        }
    }

    private void write(Values values) {
        store.putBoolean("reduceMotion", values.reduceMotion());
        store.putBoolean("animations", values.animations());
        store.putBoolean("startMaximized", values.startMaximized());
        store.put("size", values.size());
    }

    public void reset() throws BackingStoreException {
        save(DEFAULTS);
    }

    public boolean animate() {
        return current.animations() && !current.reduceMotion();
    }
}
