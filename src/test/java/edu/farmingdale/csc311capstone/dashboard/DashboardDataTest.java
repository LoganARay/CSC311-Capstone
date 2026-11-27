package edu.farmingdale.csc311capstone.dashboard;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class DashboardDataTest {
    @Test
    void noFakePlayerOrHistory() {
        var d = DashboardData.unavailable();
        assertTrue(d.player().isEmpty());
        assertTrue(d.history().isEmpty());
        assertTrue(d.rankings().isEmpty());
        assertFalse(d.historyAvailable());
        assertFalse(d.rankingsAvailable());
    }

    @Test
    void noMutableHistory() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> DashboardData.unavailable().history().clear());
    }

    @Test
    void durationFormatsHoursAndMinutes() {
        assertEquals("2h 5m", DisplayFormats.duration(Duration.ofMinutes(125)));
        assertEquals("0m", DisplayFormats.duration(Duration.ZERO));
        assertEquals("—", DisplayFormats.duration(null));
    }

    @Test
    void statisticsUseStableNumberFormatting() {
        assertEquals("12,450", DisplayFormats.number(12450));
    }

    @Test
    void nonWinOutcomesArePreserved() {
        assertEquals("Time expired", DisplayFormats.outcome("Time expired"));
        assertEquals("Completed", DisplayFormats.outcome(null));
        assertEquals("Won", DisplayFormats.outcome("Won"));
    }
}
