package edu.farmingdale.csc311capstone.Bomberman;

public class Bomb {
    private int row;
    private int column;
    private double secondsRemaining = 2.0;

    public Bomb(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public void update(double elapsedSeconds) {
        secondsRemaining -= elapsedSeconds;
    }

    public boolean isReadyToExplode() {
        return secondsRemaining <= 0;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}
