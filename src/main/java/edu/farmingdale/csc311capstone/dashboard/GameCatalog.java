package edu.farmingdale.csc311capstone.dashboard;

import java.util.*;
import java.util.random.RandomGenerator;

public final class GameCatalog {
    private GameCatalog() {}

    public static final List<String> CATEGORIES =
            List.of("All games", "Arcade", "Puzzle", "Action", "Card", "Multiplayer");
    public static final List<GameDefinition> GAMES =
            List.of(
                    game(
                            "pong",
                            "Pong",
                            "The original head-to-head rivalry.",
                            "pong-view.fxml",
                            "Arcade",
                            "#5DDDD3",
                            "Multiplayer"),
                    game(
                            "pacman",
                            "Pac-Man",
                            "Chase the dots. Dodge the ghosts.",
                            "pacman-view.fxml",
                            "Arcade",
                            "#F4C65D"),
                    game(
                            "tetris",
                            "Tetris",
                            "Find your flow, one block at a time.",
                            "tetris-home.fxml",
                            "Puzzle",
                            "#A78BFA"),
                    game(
                            "snake",
                            "Snake",
                            "One more bite. One less way out.",
                            "snake-view.fxml",
                            "Arcade",
                            "#A3E66E"),
                    game(
                            "invaders",
                            "Space Invaders",
                            "Hold the line. Save the planet.",
                            "spaceinvaders-view.fxml",
                            "Action",
                            "#F582BD"),
                    game(
                            "blackjack",
                            "Blackjack",
                            "Beat the house with strategy.",
                            "blackjack-view.fxml",
                            "Card",
                            "#5DDDD3"),
                    game(
                            "breakout",
                            "Breakout",
                            "Break bricks. Build a new best.",
                            "breakout-view.fxml",
                            "Arcade",
                            "#A78BFA"),
                    game(
                            "bomberman",
                            "Bomberman",
                            "Explore the arena. Plan your next move.",
                            "bomberman-view.fxml",
                            "Action",
                            "#F582BD"));

    private static GameDefinition game(
            String id,
            String title,
            String description,
            String route,
            String category,
            String accent,
            String... extra) {
        Set<String> tags = new HashSet<>(List.of(extra));
        tags.add(category);
        return new GameDefinition(id, title, description, route, category, tags, accent);
    }

    public static List<GameDefinition> search(String query, String category, String sort) {
        String needle = Objects.requireNonNullElse(query, "").strip().toLowerCase(Locale.ROOT);
        var stream =
                GAMES.stream()
                        .filter(
                                g ->
                                        (g.title()
                                                        + " "
                                                        + g.description()
                                                        + " "
                                                        + String.join(" ", g.tags()))
                                                .toLowerCase(Locale.ROOT)
                                                .contains(needle))
                        .filter(
                                g ->
                                        category == null
                                                || category.equals("All games")
                                                || g.tags().contains(category));
        if ("Name A–Z".equals(sort))
            stream = stream.sorted(Comparator.comparing(GameDefinition::title));
        if ("Name Z–A".equals(sort))
            stream = stream.sorted(Comparator.comparing(GameDefinition::title).reversed());
        return stream.toList();
    }

    public static GameDefinition random(RandomGenerator random) {
        return GAMES.get(random.nextInt(GAMES.size()));
    }

    public static GameDefinition byId(String id) {
        return GAMES.stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow();
    }
}
