public class CheckersGame {
    private Piece[][] board;
    private String currentTurn; // "RED" or "BLACK"

    public CheckersGame() {
        board = new Piece[8][8];
        currentTurn = "RED";
        setupBoard();
    }

    private void setupBoard() {
        // RED pieces
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 1) {
                    board[r][c] = new Piece(r, c, "RED");
                }
            }
        }
        // BLACK pieces
        for (int r = 5; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 1) {
                    board[r][c] = new Piece(r, c, "BLACK");
                }
            }
        }
    }

    public boolean move(int fr, int fc, int tr, int tc) {
        if (!inBounds(fr, fc) || !inBounds(tr, tc)) return false;

        Piece p = board[fr][fc];

        if (p == null) return false;
        if (!p.getColor().equals(currentTurn)) return false;
        if (board[tr][tc] != null) return false;

        int rowDiff = tr - fr;
        int colDiff = Math.abs(tc - fc);

        // Regular move
        if (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 1) {
            if (isForwardMove(p, rowDiff)) {
                board[tr][tc] = p;
                board[fr][fc] = null;
                p.move(tr, tc);
                checkKing(p);
                switchTurn();
                return true;
            }
        }

        // Capture
        if (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 2) {
            int midRow = (fr + tr) / 2;
            int midCol = (fc + tc) / 2;
            Piece middle = board[midRow][midCol];

            if (middle != null && !middle.getColor().equals(p.getColor())) {
                if (isForwardMove(p, rowDiff)) {
                    board[midRow][midCol] = null;
                    board[tr][tc] = p;
                    board[fr][fc] = null;
                    p.move(tr, tc);
                    checkKing(p);
                    if (!canJump(p)) {
                        switchTurn();
                    }

                    String winner = checkWinner();
                    if (winner != null) {
                        System.out.println("Winner: " + winner);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }

    private boolean isForwardMove(Piece p, int rowDiff) {
        if (p.isKing()) return true;
        if (p.getColor().equals("RED") && rowDiff > 0) return true;
        if (p.getColor().equals("BLACK") && rowDiff < 0) return true;

        return false;
    }

    private boolean canJump(Piece p) {
        int r = p.getRow();
        int c = p.getCol();
        int[][] directions = { {2, 2}, {2, -2}, {-2, 2}, {-2, -2}};

        for (int[] d : directions) {
            int tr = r + d[0];
            int tc = c + d[1];
            int midRow = r + d[0] / 2;
            int midCol = c + d[1] / 2;

            if (inBounds(tr, tc) && board[tr][tc] == null) {
                Piece middle = board[midRow][midCol];
                if (middle != null && !middle.getColor().equals(p.getColor())) {
                    if (isForwardMove(p, d[0])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void switchTurn() {
        currentTurn = currentTurn.equals("RED") ? "BLACK" : "RED";
    }

    private void checkKing(Piece p) {
        if (p.getColor().equals("RED") && p.getRow() == 7) {
            p.makeKing();
        }
        if (p.getColor().equals("BLACK") && p.getRow() == 0) {
            p.makeKing();
        }
    }

    public String checkWinner() {
        boolean redExists = false;
        boolean blackExists = false;
        boolean redCanMove = false;
        boolean blackCanMove = false;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null) {
                    if (p.getColor().equals("RED")) {
                        redExists = true;
                        if (canMoveOrJump(p)) redCanMove = true;
                    }
                    if (p.getColor().equals("BLACK")) {
                        blackExists = true;
                        if (canMoveOrJump(p)) blackCanMove = true;
                    }
                }
            }
        }
        if (!redExists) return "BLACK";
        if (!blackExists) return "RED";
        if (!redCanMove && !blackCanMove) return "DRAW";
        if (!redCanMove) return "BLACK";
        if (!blackCanMove) return "RED";
        return null;
    }

    private boolean canMoveOrJump(Piece p) {
        int r = p.getRow();
        int c = p.getCol();
        int[][] directions = {
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}, // moves
                {2, 2}, {2, -2}, {-2, 2}, {-2, -2}  // jumps
        };
        for (int[] d : directions) {
            int tr = r + d[0];
            int tc = c + d[1];
            if (!inBounds(tr, tc)) continue;

            // Normal move
            if (Math.abs(d[0]) == 1 && board[tr][tc] == null) {
                if (isForwardMove(p, d[0])) return true;
            }

            // Jump
            if (Math.abs(d[0]) == 2 && board[tr][tc] == null) {
                int midRow = r + d[0] / 2;
                int midCol = c + d[1] / 2;
                Piece middle = board[midRow][midCol];
                if (middle != null && !middle.getColor().equals(p.getColor())) {
                    if (isForwardMove(p, d[0])) return true;
                }
            }
        }
        return false;
    }

    public Piece[][] getBoard() {return board;}

    public String getCurrentTurn() {return currentTurn;}
}
