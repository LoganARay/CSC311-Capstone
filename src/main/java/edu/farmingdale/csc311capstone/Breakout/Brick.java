package edu.farmingdale.csc311capstone.Breakout;

public class Brick {
    private double x;
    private double y;
    private double width;
    private double height;
    private final boolean target;

    public Brick(double x, double y, double width, double height) {
        this(x, y, width, height, false);
    }

    public Brick(double x, double y, double width, double height, boolean target) {
        this.target = target;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isTarget() {
        return target;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
