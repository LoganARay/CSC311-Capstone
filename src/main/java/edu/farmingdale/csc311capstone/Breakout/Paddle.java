package edu.farmingdale.csc311capstone.Breakout;

public class Paddle {
    private double x;
    private double y;
    private double width;
    private double height;

    public Paddle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
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

    public void move(double amount, double gameWidth) {
        x += amount;

        if (x < 0) {
            x = 0;
        }

        if (x + width > gameWidth) {
            x = gameWidth - width;
        }
    }
}
