package edu.farmingdale.csc311capstone.dashboard;

import java.util.Set;

/** Catalog metadata only; individual games retain ownership of their rules and scores. */
public record GameDefinition(
        String id,
        String title,
        String description,
        String route,
        String category,
        Set<String> tags,
        String accent) {
    public GameDefinition {
        if (id == null
                || id.isBlank()
                || title == null
                || title.isBlank()
                || description == null
                || description.isBlank()
                || route == null
                || !route.endsWith(".fxml")
                || category == null
                || category.isBlank()
                || accent == null
                || !accent.matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Game metadata must be complete");
        }
        tags = Set.copyOf(tags);
        if (!tags.contains(category))
            throw new IllegalArgumentException("Primary category must be a tag");
    }

    @Override
    public String toString() {
        return title;
    }
}
