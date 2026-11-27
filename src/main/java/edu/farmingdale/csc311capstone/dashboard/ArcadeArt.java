package edu.farmingdale.csc311capstone.dashboard;

import javafx.scene.canvas.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.*;
import javafx.scene.text.Font;

/** Original resolution-independent pixel illustrations drawn locally, without screenshot crops. */
public final class ArcadeArt extends Region {
    private static final Font PIXEL_FONT =
            Font.loadFont(
                    ArcadeArt.class
                            .getResource(
                                    "/edu/farmingdale/csc311capstone/SnakeFonts/PressStart2P-Regular.ttf")
                            .toExternalForm(),
                    19);
    private final Canvas canvas = new Canvas();
    private final String kind;

    public ArcadeArt(String kind) {
        this.kind = kind;
        getChildren().add(canvas);
        setMinSize(0, 0);
        setPrefSize(480, kind.equals("cabinet") ? 500 : 180);
        if (!kind.equals("cabinet")) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(widthProperty());
            clip.heightProperty().bind(heightProperty());
            clip.setArcWidth(18);
            clip.setArcHeight(18);
            setClip(clip);
        }
        setMouseTransparent(true);
        setAccessibleText(kind + " pixel illustration");
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#111026"));
        g.fillRect(0, 0, getWidth(), getHeight());
        double baseHeight = kind.equals("cabinet") ? 500 : 180;
        double scale = Math.min(getWidth() / 480, getHeight() / baseHeight);
        g.save();
        g.translate((getWidth() - 480 * scale) / 2, (getHeight() - baseHeight * scale) / 2);
        g.scale(scale, scale);
        if (kind.equals("cabinet")) cabinet(g);
        else draw(g);
        g.restore();
    }

    private void draw(GraphicsContext g) {
        String bg =
                switch (kind) {
                    case "snake" -> "#05251D";
                    case "blackjack" -> "#07362E";
                    default -> "#111026";
                };
        g.setFill(
                new LinearGradient(
                        0,
                        0,
                        1,
                        1,
                        true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web(bg)),
                        new Stop(1, Color.web("#080E1C"))));
        g.fillRect(0, 0, 480, 180);
        g.setStroke(Color.web(kind.equals("snake") ? "#164635" : "#23203A", .6));
        g.setLineWidth(1);
        for (int x = 0; x < 480; x += 24) g.strokeLine(x, 0, x, 180);
        for (int y = 0; y < 180; y += 24) g.strokeLine(0, y, 480, y);
        switch (kind) {
            case "pong" -> {
                g.setFill(Color.web("#C8C8E0"));
                for (int y = 4; y < 180; y += 23) g.fillRect(237, y, 4, 12);
                block(g, 74, 35, 12, 105, "#23DDE8");
                block(g, 388, 59, 12, 90, "#F965BB");
                g.setFill(Color.web("#5DDDD3", .12));
                g.fillRect(64, 30, 32, 115);
                g.setFill(Color.web("#F582BD", .12));
                g.fillRect(378, 54, 32, 100);
                sprite(g, new String[] {"0110", "1111", "1111", "0110"}, 281, 57, 5, "#FAFAFF");
                g.setFill(Color.web("#FFFFFF", .12));
                g.fillRect(262, 61, 11, 11);
            }
            case "pacman" -> {
                g.setStroke(Color.web("#343BD4"));
                g.setLineWidth(6);
                g.strokePolyline(
                        new double[] {0, 100, 75, 75, 150, 150, 345, 345, 480},
                        new double[] {22, 22, 22, 6, 6, 22, 22, 4, 4},
                        9);
                g.strokePolyline(
                        new double[] {0, 75, 75, 180, 180, 290, 290, 480},
                        new double[] {156, 156, 174, 174, 150, 150, 171, 171},
                        8);
                g.strokePolyline(
                        new double[] {430, 430, 365, 365}, new double[] {180, 120, 120, 99}, 4);
                for (int x = 195; x < 460; x += 34) {
                    g.setFill(Color.web("#FFD887"));
                    g.fillRect(x, 67, 6, 6);
                }
                pac(g, 104, 39, 90);
                ghost(g, 280, 81, 5, "#F94E76");
                ghost(g, 348, 81, 5, "#30D9E7");
                ghost(g, 416, 81, 5, "#AF83F7");
                for (int x = 22; x < 450; x += 58) {
                    g.setFill(Color.web("#F4C65D"));
                    g.fillRect(x, 128, 5, 5);
                }
            }
            case "tetris" -> {
                tet(g, 75, 139, 22, "#20D7ED", new int[][] {{0, 0}, {1, 0}, {2, 0}, {3, 0}});
                tet(g, 120, 95, 22, "#AD6FF2", new int[][] {{0, 1}, {1, 0}, {1, 1}, {2, 0}});
                tet(g, 252, 22, 22, "#FFB43E", new int[][] {{0, 0}, {0, 1}, {0, 2}, {1, 2}});
                tet(g, 296, 116, 22, "#F661AE", new int[][] {{0, 1}, {1, 0}, {1, 1}, {2, 0}});
                g.setFill(Color.web("#A78BFA", .08));
                g.fillRect(186, 11, 42, 65);
            }
            case "snake" -> {
                int[][] body = {
                    {75, 135}, {99, 135}, {99, 111}, {123, 111}, {147, 111}, {171, 111}, {195, 111},
                    {219, 111}, {243, 111}, {243, 87}, {243, 63}, {267, 63}, {291, 63}
                };
                for (int[] b : body) block(g, b[0], b[1], 25, 25, "#A3E64A");
                g.setFill(Color.web("#183D16"));
                g.fillRect(303, 67, 5, 5);
                g.fillRect(303, 79, 5, 5);
                sprite(
                        g,
                        new String[] {"001100", "011110", "111111", "111111", "011110", "001100"},
                        367,
                        82,
                        7,
                        "#F54D65");
                block(g, 385, 69, 7, 18, "#77B656");
                block(g, 392, 64, 14, 7, "#4DDA84");
            }
            case "invaders" -> {
                stars(g, 180);
                for (int i = 0; i < 3; i++) alien(g, 89 + i * 113, 30, 5, "#F967AC");
                sprite(
                        g,
                        new String[] {
                            "00000100000",
                            "00001110000",
                            "00001110000",
                            "01111111110",
                            "11111111111",
                            "11111111111",
                            "11100000111"
                        },
                        208,
                        126,
                        5,
                        "#BACBFF");
                block(g, 234, 95, 4, 16, "#F4C65D");
            }
            case "blackjack" -> {
                g.save();
                g.translate(160, 86);
                g.rotate(-14);
                card(g, -47, -70, "A", "♠", "#111827");
                g.restore();
                g.save();
                g.translate(282, 86);
                g.rotate(14);
                card(g, -47, -70, "K", "♥", "#E43E5D");
                g.restore();
                chip(g, 85, 149, "#EE5278");
                chip(g, 380, 149, "#72A9D0");
                chip(g, 76, 139, "#EE5278");
            }
            case "breakout" -> {
                String[] colors = {"#F65C9E", "#23D3E7", "#F6B344"};
                for (int row = 0; row < 3; row++)
                    for (int col = 0; col < 9; col++)
                        if (row < 2 || col < 3 || col > 6)
                            block(g, 24 + col * 48, 15 + row * 23, 45, 19, colors[row]);
                block(g, 180, 154, 117, 12, "#AC76F5");
                for (int i = 0; i < 4; i++) {
                    g.setFill(Color.web("#AC76F5", .2 + i * .15));
                    g.fillRect(258 + i * 11, 129 - i * 11, 5, 5);
                }
                sprite(g, new String[] {"0110", "1111", "1111", "0110"}, 298, 83, 5, "#F2F2FF");
            }
            case "bomberman" -> {
                for (int x : new int[] {16, 400})
                    for (int y : new int[] {15, 100}) {
                        block(g, x, y, 62, 62, "#5D3E84");
                        block(g, x + 7, y + 7, 18, 8, "#8260A5");
                    }
                sprite(
                        g,
                        new String[] {
                            "00003330000",
                            "00011111000",
                            "00111111100",
                            "00122222100",
                            "00124242100",
                            "00122222100",
                            "00011111000",
                            "03355555330",
                            "33355555333",
                            "00055555000",
                            "00011211000",
                            "00333033300",
                            "03333033330"
                        },
                        164,
                        18,
                        10,
                        "#F8EDF9");
                g.setFill(Color.web("#36394C"));
                g.fillOval(310, 93, 62, 62);
                g.setFill(Color.web("#565B71"));
                g.fillRect(324, 101, 18, 13);
                g.setStroke(Color.web("#D8B17B"));
                g.setLineWidth(5);
                g.strokePolyline(
                        new double[] {338, 338, 352, 358}, new double[] {95, 79, 71, 52}, 4);
                block(g, 348, 50, 24, 4, "#F4C65D");
                block(g, 358, 40, 4, 24, "#F4C65D");
            }
        }
        g.setFill(
                new LinearGradient(
                        0,
                        0,
                        0,
                        1,
                        true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, Color.TRANSPARENT),
                        new Stop(1, Color.web("#0A0D18", .23))));
        g.fillRect(0, 0, 480, 180);
    }

    private static void stars(GraphicsContext g, int height) {
        for (int i = 0; i < 32; i++) {
            g.setFill(Color.web(i % 3 == 0 ? "#F4C65D" : "#C9C7EA", .7));
            g.fillRect((i * 137 + 17) % 470, (i * 73 + 11) % height, i % 4 == 0 ? 3 : 2, 2);
        }
    }

    private static void block(
            GraphicsContext g, double x, double y, double w, double h, String color) {
        Color c = Color.web(color);
        g.setFill(c);
        g.fillRect(x, y, w, h);
        g.setFill(c.interpolate(Color.WHITE, .3));
        g.fillRect(x + 2, y + 2, w - 4, 3);
        g.setFill(c.interpolate(Color.BLACK, .25));
        g.fillRect(x + 2, y + h - 4, w - 4, 3);
    }

    private static void sprite(
            GraphicsContext g, String[] rows, double x, double y, double s, String color) {
        String[] palette = {"#00000000", color, "#FFD9B4", "#F264AC", "#252843", "#5F63D9"};
        for (int r = 0; r < rows.length; r++)
            for (int c = 0; c < rows[r].length(); c++) {
                int v = Character.digit(rows[r].charAt(c), 10);
                if (v > 0) {
                    g.setFill(Color.web(palette[v]));
                    g.fillRect(x + c * s, y + r * s, s, s);
                }
            }
    }

    private static void pac(GraphicsContext g, double x, double y, double size) {
        sprite(
                g,
                new String[] {
                    "000111110000",
                    "001111111100",
                    "011111111110",
                    "111111111100",
                    "111111110000",
                    "111111000000",
                    "111111110000",
                    "111111111100",
                    "011111111110",
                    "001111111100",
                    "000111110000"
                },
                x,
                y,
                size / 12,
                "#FFDA24");
    }

    private static void ghost(GraphicsContext g, double x, double y, double s, String color) {
        sprite(
                g,
                new String[] {
                    "000111000",
                    "001111100",
                    "011111110",
                    "111111111",
                    "112211221",
                    "112211221",
                    "114211421",
                    "111111111",
                    "111111111",
                    "110101011"
                },
                x,
                y,
                s,
                color);
        g.setFill(Color.web("#F8F7FF"));
        g.fillRect(x + 2 * s, y + 4 * s, 2 * s, 3 * s);
        g.fillRect(x + 6 * s, y + 4 * s, 2 * s, 3 * s);
        g.setFill(Color.web("#31328A"));
        g.fillRect(x + 3 * s, y + 5 * s, s, 2 * s);
        g.fillRect(x + 7 * s, y + 5 * s, s, 2 * s);
    }

    private static void alien(GraphicsContext g, double x, double y, double s, String color) {
        sprite(
                g,
                new String[] {
                    "00100000100",
                    "00010001000",
                    "00111111100",
                    "01101110110",
                    "11111111111",
                    "10111111101",
                    "10100000101",
                    "00011011000"
                },
                x,
                y,
                s,
                color);
    }

    private static void tet(GraphicsContext g, int x, int y, int s, String color, int[][] cells) {
        for (int[] p : cells) block(g, x + p[0] * (s + 2), y + p[1] * (s + 2), s, s, color);
    }

    private static void card(
            GraphicsContext g, int x, int y, String rank, String suit, String color) {
        g.setFill(Color.web("#F8F0D9"));
        g.fillRoundRect(x, y, 92, 130, 7, 7);
        g.setFill(Color.web(color));
        g.setFont(Font.font("Serif", 27));
        g.fillText(rank, x + 10, y + 28);
        g.setFont(Font.font("Serif", 65));
        g.fillText(suit, x + 16, y + 99);
    }

    private static void chip(GraphicsContext g, int x, int y, String color) {
        g.setFill(Color.web(color));
        g.fillOval(x - 30, y - 14, 60, 28);
        g.setStroke(Color.web("#F7F3E9"));
        g.setLineWidth(4);
        g.setLineDashes(8, 7);
        g.strokeOval(x - 25, y - 11, 50, 22);
        g.setLineDashes();
    }

    private void cabinet(GraphicsContext g) {
        g.setFill(
                new LinearGradient(
                        0,
                        0,
                        0,
                        1,
                        true,
                        CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#151129")),
                        new Stop(.7, Color.web("#24103D")),
                        new Stop(1, Color.web("#100E21"))));
        g.fillRect(0, 0, 480, 500);
        stars(g, 390);
        g.setStroke(Color.web("#7E379D", .4));
        g.setLineWidth(2);
        for (int x = -400; x < 1000; x += 75) g.strokeLine(240 + (x - 240) * .18, 350, x, 500);
        for (int y = 355; y < 500; y += (y - 330) / 3) g.strokeLine(0, y, 480, y);
        for (int i = 0; i < 9; i++) {
            g.setFill(Color.web("#140D25"));
            g.fillRect(i * 62 - 15, 300 + (i % 3) * 17, 45, 100);
        }
        // Three-quarter cabinet silhouette, with stepped neon trim and a raised marquee.
        poly(
                g,
                "#1A1430",
                new double[] {130, 345, 379, 383, 342, 342, 130},
                new double[] {70, 53, 80, 383, 454, 484, 452});
        poly(
                g,
                "#30204F",
                new double[] {130, 298, 328, 295, 329, 329, 112, 112, 145, 123},
                new double[] {70, 77, 129, 155, 330, 457, 450, 332, 143, 126});
        g.setStroke(Color.web("#B563F0"));
        g.setLineWidth(7);
        g.strokePolyline(
                new double[] {130, 298, 328, 295, 329, 329},
                new double[] {70, 77, 129, 155, 330, 457},
                6);
        g.strokePolyline(
                new double[] {130, 123, 145, 112, 112}, new double[] {70, 126, 143, 332, 450}, 5);
        poly(g, "#0F0F25", new double[] {143, 289, 303, 153}, new double[] {88, 94, 123, 120});
        g.setFill(Color.web("#F4C65D"));
        g.setFont(PIXEL_FONT);
        g.fillText("ARCADE", 162, 117);
        poly(g, "#0C182A", new double[] {157, 280, 294, 144}, new double[] {155, 162, 286, 277});
        g.setStroke(Color.web("#4F3F9E"));
        g.setLineWidth(5);
        g.strokePolygon(new double[] {157, 280, 294, 144}, new double[] {155, 162, 286, 277}, 4);
        alien(g, 181, 196, 7, "#5DDDD3");
        poly(g, "#7040A4", new double[] {144, 294, 321, 119}, new double[] {288, 295, 330, 323});
        g.setFill(Color.web("#15122E"));
        g.fillOval(155, 312, 38, 11);
        block(g, 170, 283, 6, 31, "#5DDDD3");
        g.setFill(Color.web("#F661AE"));
        g.fillOval(161, 269, 24, 25);
        for (int i = 0; i < 3; i++) {
            g.setFill(Color.web("#F4C65D"));
            g.fillOval(222 + i * 29, 310 + (i % 2) * -6, 20, 9);
        }
        block(g, 174, 371, 80, 51, "#1C1630");
        block(g, 206, 378, 23, 7, "#73459F");
        pac(g, 370, 20, 46);
        ghost(g, 50, 242, 4, "#8B56B4");
        g.setStroke(Color.web("#CE5BEC", .6));
        g.setLineWidth(5);
        g.strokeLine(22, 120, 83, 145);
        g.strokeLine(376, 188, 449, 167);
    }

    private static void poly(GraphicsContext g, String color, double[] x, double[] y) {
        g.setFill(Color.web(color));
        g.fillPolygon(x, y, x.length);
    }
}
