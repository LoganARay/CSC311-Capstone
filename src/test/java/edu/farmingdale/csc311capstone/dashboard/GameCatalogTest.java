package edu.farmingdale.csc311capstone.dashboard;

import static org.junit.jupiter.api.Assertions.*;

import edu.farmingdale.csc311capstone.HelloApplication;

import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Stream;

class GameCatalogTest {
    @Test
    void exactlyEightUniqueGames() {
        assertEquals(8, GameCatalog.GAMES.size());
        assertEquals(8, GameCatalog.GAMES.stream().map(GameDefinition::id).distinct().count());
        assertEquals(8, GameCatalog.GAMES.stream().map(GameDefinition::route).distinct().count());
    }

    @TestFactory
    Stream<DynamicTest> allRoutesExist() {
        return GameCatalog.GAMES.stream()
                .map(
                        g ->
                                DynamicTest.dynamicTest(
                                        g.title(),
                                        () ->
                                                assertNotNull(
                                                        HelloApplication.class.getResource(
                                                                g.route()))));
    }

    @Test
    void doesNotIncludeDigDug() {
        assertTrue(GameCatalog.GAMES.stream().noneMatch(g -> g.id().contains("dig")));
    }

    @Test
    void textSearchIgnoresCaseAndWhitespace() {
        assertEquals(
                List.of(GameCatalog.byId("pacman")),
                GameCatalog.search("  PAC-MAN ", "All games", "Featured order"));
    }

    @Test
    void descriptionSearch() {
        assertEquals("pong", GameCatalog.search("rivalry", null, null).getFirst().id());
    }

    @Test
    void searchCategoryAndQueryCombine() {
        assertTrue(GameCatalog.search("Pong", "Puzzle", null).isEmpty());
    }

    @Test
    void allFiltersUseCatalogMetadata() {
        for (String category : GameCatalog.CATEGORIES) {
            var found = GameCatalog.search("", category, null);
            assertFalse(found.isEmpty());
            assertTrue(
                    found.stream()
                            .allMatch(
                                    g ->
                                            category.equals("All games")
                                                    || g.tags().contains(category)));
        }
    }

    @Test
    void multiplayerOnlyReturnsPong() {
        assertEquals(
                List.of(GameCatalog.byId("pong")), GameCatalog.search(null, "Multiplayer", null));
    }

    @Test
    void noResults() {
        assertTrue(GameCatalog.search("no-such-game", "All games", null).isEmpty());
    }

    @Test
    void alphabeticalSortAndReverse() {
        var asc = GameCatalog.search(null, null, "Name A–Z");
        var desc = GameCatalog.search(null, null, "Name Z–A");
        assertEquals("Blackjack", asc.getFirst().title());
        assertEquals(asc.reversed(), desc);
    }

    @Test
    void defaultOrderPreserved() {
        assertEquals(GameCatalog.GAMES, GameCatalog.search(null, null, null));
    }

    @Test
    void randomAlwaysReturnsAvailableGamesAndReachesAllEight() {
        Set<String> seen = new HashSet<>();
        Random random = new Random(311);
        for (int i = 0; i < 1000; i++) {
            var g = GameCatalog.random(random);
            assertTrue(GameCatalog.GAMES.contains(g));
            seen.add(g.id());
        }
        assertEquals(8, seen.size());
    }

    @Test
    void immutableCatalog() {
        assertThrows(UnsupportedOperationException.class, () -> GameCatalog.GAMES.clear());
        assertThrows(
                UnsupportedOperationException.class, () -> GameCatalog.byId("pong").tags().clear());
    }

    @Test
    void validatesMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new GameDefinition(
                                "",
                                "Game",
                                "Description",
                                "game.fxml",
                                "Arcade",
                                Set.of("Arcade"),
                                "#FFFFFF"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new GameDefinition(
                                "id",
                                "Game",
                                "Description",
                                "game.html",
                                "Arcade",
                                Set.of("Arcade"),
                                "#FFFFFF"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new GameDefinition(
                                "id",
                                "Game",
                                "Description",
                                "game.fxml",
                                "Arcade",
                                Set.of("Puzzle"),
                                "#FFFFFF"));
    }
}
