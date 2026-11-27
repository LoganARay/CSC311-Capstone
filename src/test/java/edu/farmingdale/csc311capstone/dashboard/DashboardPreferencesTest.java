package edu.farmingdale.csc311capstone.dashboard;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.prefs.Preferences;

class DashboardPreferencesTest {
    private Preferences node;

    @BeforeEach
    void setup() {
        node =
                Preferences.userRoot()
                        .node("edu/farmingdale/csc311capstone/test-" + UUID.randomUUID());
    }

    @AfterEach
    void cleanup() throws Exception {
        node.removeNode();
        node.flush();
    }

    @Test
    void defaults() {
        assertEquals(DashboardPreferences.DEFAULTS, new DashboardPreferences(node).current());
    }

    @Test
    void roundTrip() throws Exception {
        var expected = new DashboardPreferences.Values(true, false, true, "1024 × 768");
        new DashboardPreferences(node).save(expected);
        assertEquals(expected, new DashboardPreferences(node).current());
    }

    @Test
    void resetPersists() throws Exception {
        var p = new DashboardPreferences(node);
        p.save(new DashboardPreferences.Values(true, false, true, "1280 × 800"));
        p.reset();
        assertEquals(DashboardPreferences.DEFAULTS, new DashboardPreferences(node).current());
    }

    @Test
    void reduceMotionOverridesAnimations() throws Exception {
        var p = new DashboardPreferences(node);
        p.save(new DashboardPreferences.Values(true, true, false, "1440 × 900"));
        assertFalse(p.animate());
    }

    @Test
    void rejectsUnsupportedSize() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new DashboardPreferences(node)
                                .save(
                                        new DashboardPreferences.Values(
                                                false, true, false, "10 × 10")));
    }

    @Test
    void invalidStoredSizeFallsBack() {
        node.put("size", "corrupt");
        assertEquals("1440 × 900", new DashboardPreferences(node).current().size());
    }
}
