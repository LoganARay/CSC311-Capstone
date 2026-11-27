package edu.farmingdale.csc311capstone.dashboard;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

/** Small original line icons; no platform-specific emoji fonts. */
public final class Icons {
    private Icons() {}

    public static Group of(String name) {
        String path =
                switch (name) {
                    case "home" ->
                            "M3 10 L12 3 L21 10 M5 9 L5 21 L10 21 L10 14 L14 14 L14 21 L19 21 L19"
                                + " 9";
                    case "trophy" ->
                            "M7 3 L17 3 L17 10 Q17 15 12 15 Q7 15 7 10 Z M7 5 L3 5 L3 9 Q3 12 8 12"
                                + " M17 5 L21 5 L21 9 Q21 12 16 12 M12 15 L12 21 M7 21 L17 21";
                    case "stats" -> "M4 21 L4 13 M10 21 L10 4 M16 21 L16 9 M22 21 L22 16";
                    case "profile" ->
                            "M8 7 A4 4 0 1 0 16 7 A4 4 0 1 0 8 7 M3 22 L3 19 Q3 13 12 13 Q21 13 21"
                                + " 19 L21 22";
                    case "settings" ->
                            "M9 3 L15 3 L16 6 L20 7 L22 12 L20 17 L16 18 L15 21 L9 21 L8 18 L4 17"
                                + " L2 12 L4 7 L8 6 Z M8 12 A4 4 0 1 0 16 12 A4 4 0 1 0 8 12";
                    case "clock" ->
                            "M2 12 A10 10 0 1 0 22 12 A10 10 0 1 0 2 12 M12 6 L12 12 L17 15";
                    case "star" -> "M12 2 L15 8 L22 9 L17 14 L18 22 L12 18 L6 22 L7 14 L2 9 L9 8 Z";
                    case "search" -> "M2 9 A7 7 0 1 0 16 9 A7 7 0 1 0 2 9 M15 15 L22 22";
                    case "lock" ->
                            "M5 10 L19 10 L19 22 L5 22 Z M8 10 L8 6 A4 4 0 0 1 16 6 L16 10 M12 15"
                                + " L12 18";
                    default ->
                            "M7 6 L17 6 Q20 6 21 11 L23 18 Q23 22 19 20 L15 17 L9 17 L5 20 Q1 22 1"
                                + " 18 L3 11 Q4 6 7 6 Z M5 11 L11 11 M8 8 L8 14 M17 11 L17 11.2 M20"
                                + " 14 L20 14.2";
                };
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#B4AECA"));
        p.setStrokeWidth(1.8);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        p.getStyleClass().add("line-icon");
        Group group = new Group(p);
        group.setMouseTransparent(true);
        return group;
    }
}
