package edu.farmingdale.csc311capstone.dashboard;

import java.time.*;
import java.util.*;

/**
 * Immutable presentation data. A future backend adapter supplies this snapshot to the shell. No
 * database, accounts, sessions, score aggregation or invented player records live here.
 */
public record DashboardData(
        Optional<Player> player,
        List<Match> history,
        List<Ranking> rankings,
        boolean historyAvailable,
        boolean rankingsAvailable) {
    public DashboardData {
        player = Objects.requireNonNull(player);
        history = List.copyOf(history);
        rankings = List.copyOf(rankings);
    }

    public static DashboardData unavailable() {
        return new DashboardData(Optional.empty(), List.of(), List.of(), false, false);
    }

    public record Player(String displayName, String username, LocalDate memberSince, String bio) {}

    public record Match(
            String gameId,
            String mode,
            long score,
            String outcome,
            Duration duration,
            Instant playedAt) {}

    public record Ranking(
            int rank, String username, String gameId, String mode, long score, LocalDate date) {}
}
