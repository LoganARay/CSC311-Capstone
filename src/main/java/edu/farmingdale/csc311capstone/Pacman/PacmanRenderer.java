package edu.farmingdale.csc311capstone.Pacman;

import static edu.farmingdale.csc311capstone.Pacman.PacmanMechanics.*;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/** Original vector characters and maze art; model positions are never changed by rendering. */
final class PacmanRenderer {
    private static final double TILE = 32;
    private static final Color GOLD = Color.web("#ffe66b"),
            WALL_FILL = Color.web("#071937"),
            WALL_EDGE = Color.web("#3575ff");
    private static final Color[] GHOST_COLORS = {
        Color.web("#ff5c72"), Color.web("#ff9cdb"), Color.web("#5ce7e5"), Color.web("#ffb55b")
    };

    private static class Bonus {
        double x, y, life = 1;
        String text;

        Bonus(double x, double y, String text) {
            this.x = x;
            this.y = y;
            this.text = text;
        }
    }

    private final Canvas canvas;
    private final List<Bonus> bonuses = new ArrayList<>();
    private double flash;

    PacmanRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    void clear() {
        bonuses.clear();
        flash = 0;
    }

    void effect(Event event, PacmanMechanics m) {
        if (event == Event.POWER || event == Event.LEVEL) flash = .45;
        if (event == Event.CAPTURE || event == Event.FRUIT) {
            if (bonuses.size() < 10)
                bonuses.add(
                        new Bonus(
                                (m.getPlayerVisualCol() + .5) * TILE,
                                (m.getPlayerVisualRow() + .5) * TILE,
                                event == Event.CAPTURE ? "GHOST +" : "FRUIT +"));
        }
    }

    void draw(PacmanMechanics m, double time, double dt, boolean menu) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double width = canvas.getWidth(), height = canvas.getHeight();
        g.setFill(Color.web("#080b1a"));
        g.fillRect(0, 0, width, height);
        double w = m.getColumns() * TILE,
                h = m.getRows() * TILE,
                scale = Math.min(width / w, height / h);
        g.save();
        g.translate((width - w * scale) / 2, (height - h * scale) / 2);
        g.scale(scale, scale);
        for (int r = 0; r < m.getRows(); r++)
            for (int c = 0; c < m.getColumns(); c++) {
                double x = c * TILE, y = r * TILE;
                char tile = m.getTile(r, c);
                if (tile == WALL) {
                    g.setFill(WALL_FILL);
                    g.fillRect(x, y, TILE, TILE);
                    g.setStroke(WALL_EDGE);
                    g.setLineWidth(2);
                    boolean top = !m.isWall(r - 1, c),
                            bottom = !m.isWall(r + 1, c),
                            left = !m.isWall(r, c - 1),
                            right = !m.isWall(r, c + 1);
                    double x1 = x + (left ? 3 : 0),
                            x2 = x + TILE - (right ? 3 : 0),
                            y1 = y + (top ? 3 : 0),
                            y2 = y + TILE - (bottom ? 3 : 0);
                    if (top) g.strokeLine(x1, y + 3, x2, y + 3);
                    if (bottom) g.strokeLine(x1, y + TILE - 3, x2, y + TILE - 3);
                    if (left) g.strokeLine(x + 3, y1, x + 3, y2);
                    if (right) g.strokeLine(x + TILE - 3, y1, x + TILE - 3, y2);
                } else if (tile == PELLET) {
                    g.setFill(Color.web("#ffe6bd"));
                    g.fillOval(x + 14, y + 14, 4, 4);
                } else if (tile == POWER) {
                    double radius = 5.5 + Math.sin(time * 6) * 1.5;
                    g.setFill(Color.rgb(255, 230, 107, .12));
                    g.fillOval(x + 5, y + 5, 22, 22);
                    g.setFill(GOLD);
                    g.fillOval(x + 16 - radius, y + 16 - radius, radius * 2, radius * 2);
                }
            }
        g.setStroke(Color.web("#1f458b"));
        g.setLineWidth(1);
        g.strokeRoundRect(1, 1, w - 2, h - 2, 10, 10);
        if (m.getFruitSeconds() > 0)
            fruit(
                    g,
                    (m.getFruitCell().col() + .5) * TILE,
                    (m.getFruitCell().row() + .5) * TILE,
                    time);
        for (int i = 0; i < 4; i++) {
            double x = (m.getGhostVisualCol(i) + .5) * TILE,
                    y = (m.getGhostVisualRow(i) + .5) * TILE;
            ghost(
                    g,
                    x,
                    y,
                    GHOST_COLORS[i],
                    m.getGhostState(i),
                    m.getGhostDirection(i),
                    m.getFrightenedSeconds(),
                    time);
            if (x < 16)
                ghost(
                        g,
                        x + w,
                        y,
                        GHOST_COLORS[i],
                        m.getGhostState(i),
                        m.getGhostDirection(i),
                        m.getFrightenedSeconds(),
                        time);
            if (x > w - 16)
                ghost(
                        g,
                        x - w,
                        y,
                        GHOST_COLORS[i],
                        m.getGhostState(i),
                        m.getGhostDirection(i),
                        m.getFrightenedSeconds(),
                        time);
        }
        double px = (m.getPlayerVisualCol() + .5) * TILE, py = (m.getPlayerVisualRow() + .5) * TILE;
        double mouth = menu ? 25 : 8 + Math.abs(Math.sin(time * 13)) * 30;
        if (m.getState() == State.LIFE_LOST) {
            double age = 2 - m.getTransitionSeconds();
            g.setStroke(GOLD.deriveColor(0, 1, 1, Math.max(0, 1 - age / 2)));
            g.setLineWidth(3);
            double radius = age * 24;
            g.strokeOval(
                    (m.getDeathCol() + .5) * TILE - radius,
                    (m.getDeathRow() + .5) * TILE - radius,
                    radius * 2,
                    radius * 2);
        }
        pacman(g, px, py, m.getDirection(), mouth);
        if (px < 16) pacman(g, px + w, py, m.getDirection(), mouth);
        if (px > w - 16) pacman(g, px - w, py, m.getDirection(), mouth);
        bonuses.removeIf(b -> b.life <= 0);
        g.setFont(Font.font("Monospaced", 12));
        g.setTextAlign(TextAlignment.CENTER);
        for (Bonus b : bonuses) {
            b.life -= dt;
            b.y -= dt * 22;
            g.setFill(GOLD.deriveColor(0, 1, 1, Math.max(0, b.life)));
            g.fillText(b.text, b.x, b.y);
        }
        if (flash > 0) {
            g.setFill(Color.rgb(255, 230, 107, flash * .22));
            g.fillRect(0, 0, w, h);
            flash = Math.max(0, flash - dt);
        }
        if (!menu
                && (m.getState() == State.READY
                        || m.getState() == State.LIFE_LOST
                        || m.getState() == State.LEVEL_COMPLETE)) {
            String title =
                    m.getState() == State.LEVEL_COMPLETE
                            ? "LEVEL CLEAR"
                            : m.getState() == State.LIFE_LOST ? "LIFE LOST" : "READY!";
            String detail =
                    m.getState() == State.LEVEL_COMPLETE
                            ? "NEXT MAZE / KEEP YOUR SCORE"
                            : "GET READY  " + (int) Math.ceil(m.getTransitionSeconds());
            g.setFill(Color.rgb(8, 11, 26, .93));
            g.fillRoundRect(w / 2 - 155, h / 2 - 50, 310, 100, 12, 12);
            g.setStroke(GOLD);
            g.strokeRoundRect(w / 2 - 155, h / 2 - 50, 310, 100, 12, 12);
            g.setFill(GOLD);
            g.setFont(Font.font("Monospaced", 27));
            g.fillText(title, w / 2, h / 2 - 3);
            g.setFill(Color.web("#c1c9e5"));
            g.setFont(Font.font("Monospaced", 12));
            g.fillText(detail, w / 2, h / 2 + 24);
        }
        g.restore();
    }

    private void pacman(GraphicsContext g, double x, double y, Direction d, double mouth) {
        double rotation =
                switch (d) {
                    case UP -> 90;
                    case DOWN -> 270;
                    case LEFT -> 180;
                    default -> 0;
                };
        g.setFill(Color.rgb(255, 230, 107, .10));
        g.fillOval(x - 16, y - 16, 32, 32);
        g.setFill(GOLD);
        g.fillArc(x - 12, y - 12, 24, 24, rotation + mouth, 360 - 2 * mouth, ArcType.ROUND);
    }

    private void ghost(
            GraphicsContext g,
            double x,
            double y,
            Color color,
            GhostState state,
            Direction direction,
            double frightened,
            double time) {
        boolean scared = state == GhostState.FRIGHTENED, returning = state == GhostState.RETURNING;
        if (scared)
            color =
                    frightened < 2 && Math.sin(time * 16) > 0
                            ? Color.web("#f2f5ff")
                            : Color.web("#3358e5");
        if (!returning) {
            g.setFill(color);
            g.fillArc(x - 12, y - 12, 24, 25, 0, 180, ArcType.ROUND);
            g.fillRect(x - 12, y, 24, 9);
            double wave = Math.sin(time * 9) * 2;
            g.fillPolygon(
                    new double[] {x - 12, x - 8, x - 4, x, x + 4, x + 8, x + 12, x + 12, x - 12},
                    new double[] {
                        y + 10,
                        y + 7 + wave,
                        y + 10,
                        y + 7 - wave,
                        y + 10,
                        y + 7 + wave,
                        y + 10,
                        y,
                        y
                    },
                    9);
        }
        if (scared) {
            g.setFill(Color.web("#ffdba5"));
            g.fillRect(x - 6, y - 3, 3, 3);
            g.fillRect(x + 3, y - 3, 3, 3);
            g.setStroke(Color.web("#ffdba5"));
            g.setLineWidth(1.5);
            g.strokePolyline(
                    new double[] {x - 7, x - 3, x + 1, x + 5, x + 8},
                    new double[] {y + 5, y + 3, y + 5, y + 3, y + 5},
                    5);
        } else {
            g.setFill(Color.WHITE);
            g.fillOval(x - 8, y - 6, 7, 9);
            g.fillOval(x + 2, y - 6, 7, 9);
            g.setFill(Color.web("#15234e"));
            g.fillOval(x - 6 + direction.col * 1.5, y - 3 + direction.row * 1.5, 3.5, 4.5);
            g.fillOval(x + 4 + direction.col * 1.5, y - 3 + direction.row * 1.5, 3.5, 4.5);
        }
    }

    private void fruit(GraphicsContext g, double x, double y, double time) {
        double bob = Math.sin(time * 5) * 2;
        g.setStroke(Color.web("#5ee5a0"));
        g.setLineWidth(2);
        g.strokeLine(x - 4, y - 3 + bob, x + 4, y - 12 + bob);
        g.strokeLine(x + 5, y - 3 + bob, x + 4, y - 12 + bob);
        g.setFill(Color.web("#ff587d"));
        g.fillOval(x - 12, y - 3 + bob, 13, 13);
        g.fillOval(x + 1, y - 1 + bob, 13, 13);
        g.setFill(Color.web("#ffccd7"));
        g.fillOval(x - 9, y - 1 + bob, 3, 3);
        g.fillOval(x + 4, y + 1 + bob, 3, 3);
    }

    void drawAttract(double time) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.setFill(Color.web("#080b1a"));
        g.fillRect(0, 0, w, h);
        g.setStroke(Color.web("#142347"));
        g.setLineWidth(1);
        for (int y = 20; y < h; y += 28) g.strokeLine(0, y, w, y);
        double x = (time * 70) % (w + 220) - 100;
        for (int i = 0; i < 4; i++) {
            double gx = x - 42 - i * 40;
            if (gx < 0) gx += w + 220;
            ghost(g, gx, h * .19, GHOST_COLORS[i], GhostState.CHASE, Direction.RIGHT, 0, time);
        }
        pacman(g, x, h * .19, Direction.RIGHT, 10 + Math.abs(Math.sin(time * 12)) * 28);
        for (int p = 0; p < 9; p++) {
            g.setFill(GOLD.deriveColor(0, 1, 1, .35));
            g.fillOval(w / 2 - 120 + p * 30, h * .80, 4, 4);
        }
    }
}
