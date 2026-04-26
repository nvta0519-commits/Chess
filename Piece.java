public class Piece {
    private int row;
    private int col;
    private String color;
    private boolean isKing;

    public Piece(int row, int col, String color) {
        this.row = row;
        this.col = col;
        this.color = color;
        this.isKing = false;
    }

    public void move(int newRow, int newCol) {
        this.row = newRow;
        this.col = newCol;
    }

    public void makeKing() {
        this.isKing = true;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public String getColor() { return color; }
    public boolean isKing() { return isKing; }

    @Override
    public String toString() {
        return color.charAt(0) + (isKing ? "K" : "P");
    }
}
