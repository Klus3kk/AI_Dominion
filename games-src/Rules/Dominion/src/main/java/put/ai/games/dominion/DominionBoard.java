/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.dominion;

import put.ai.games.game.Move;
import put.ai.games.game.Player;
import put.ai.games.game.Player.Color;
import put.ai.games.game.TypicalBoard;
import put.ai.games.game.moves.MoveMove;
import put.ai.games.game.moves.PlaceMove;
import put.ai.games.game.moves.SkipMove;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

abstract class SwappingMove implements Move {

    private List<int[]> swappings = new ArrayList<>();
    private Color myColor;


    public SwappingMove(Color myColor) {
        this.myColor = myColor;
    }


    public List<int[]> getSwappings() {
        return swappings;
    }


    @Override
    public Color getColor() {
        return myColor;
    }


    public abstract int getX();


    public abstract int getY();
}

class DominionPlaceMove extends SwappingMove implements PlaceMove {

    private int x, y;


    public DominionPlaceMove(Color myColor, int x, int y) {
        super(myColor);
        this.x = x;
        this.y = y;
    }


    @Override
    public int getX() {
        return x;
    }


    @Override
    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "DominionPlaceMove{" + x + ", " + y + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DominionPlaceMove that = (DominionPlaceMove) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

class DominionSkipMove implements SkipMove {

    private Color color;


    public DominionSkipMove(Color color) {
        this.color = color;
    }


    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "DominionSkipMove{}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DominionSkipMove that = (DominionSkipMove) o;
        return color == that.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color);
    }
}

class DominionMoveMove extends SwappingMove implements MoveMove {

    private int srcX, srcY, dstX, dstY;


    public DominionMoveMove(Color myColor, int srcX, int srcY, int dstX, int dstY) {
        super(myColor);
        this.srcX = srcX;
        this.srcY = srcY;
        this.dstX = dstX;
        this.dstY = dstY;
    }


    @Override
    public int getDstX() {
        return dstX;
    }


    @Override
    public int getDstY() {
        return dstY;
    }


    @Override
    public int getSrcX() {
        return srcX;
    }


    @Override
    public int getSrcY() {
        return srcY;
    }


    @Override
    public int getX() {
        return dstX;
    }


    @Override
    public int getY() {
        return dstY;
    }

    @Override
    public String toString() {
        return "DominionMoveMove{" +
                srcX +
                ", " + srcY +
                ", " + dstX +
                ", " + dstY +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DominionMoveMove that = (DominionMoveMove) o;
        return srcX == that.srcX && srcY == that.srcY && dstX == that.dstX && dstY == that.dstY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcX, srcY, dstX, dstY);
    }
}

public class DominionBoard extends TypicalBoard {

    public DominionBoard(int boardSize) {
        super(boardSize);
        if (boardSize % 2 != 0)
            throw new IllegalArgumentException("Board size must be even");
        int h = boardSize / 2;
        state[0][0] = Color.PLAYER1;
        state[h - 1][h - 1] = Color.PLAYER1;
        state[h][h] = Color.PLAYER1;
        state[boardSize - 1][boardSize - 1] = Color.PLAYER1;
        state[boardSize - 1][0] = Color.PLAYER2;
        state[h][h - 1] = Color.PLAYER2;
        state[h - 1][h] = Color.PLAYER2;
        state[0][boardSize - 1] = Color.PLAYER2;
    }


    protected DominionBoard(DominionBoard other) {
        super(other);
    }

    /**
     * An auxiliary method for ease testing.
     */
    /*package*/ void setState(Color[][] state) {
        this.state = state;
    }


    @Override
    public DominionBoard clone() {
        return new DominionBoard(this);
    }


    @Override
    protected boolean canMove(Color color) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    private void doPlace(DominionPlaceMove move) {
        if (!isValid(move.getX(), move.getY())) {
            throw new IllegalArgumentException("Move outside the board");
        }
        if (state[move.getX()][move.getY()] != Color.EMPTY) {
            throw new IllegalArgumentException("Cell is not empty");
        }
        state[move.getX()][move.getY()] = move.getColor();
        doSwap(move);
    }


    private void undoPlace(DominionPlaceMove move) {
        if (!isValid(move.getX(), move.getY())) {
            throw new IllegalArgumentException("Move outside the board");
        }
        if (state[move.getX()][move.getY()] != move.getColor()) {
            throw new IllegalArgumentException("Cell is empty");
        }
        state[move.getX()][move.getY()] = Color.EMPTY;
        undoSwap(move);
    }


    private void doJump(DominionMoveMove move) {
        if (!isValid(move.getSrcX(), move.getSrcY()) || !isValid(move.getDstX(), move.getDstY())) {
            throw new IllegalArgumentException("Move outside the board");
        }
        if (state[move.getSrcX()][move.getSrcY()] != move.getColor()) {
            throw new IllegalArgumentException("Cell does not belong to the player");
        }
        if (state[move.getDstX()][move.getDstY()] != Color.EMPTY) {
            throw new IllegalArgumentException("Cell is not empty");
        }
        state[move.getDstX()][move.getDstY()] = state[move.getSrcX()][move.getSrcY()];
        state[move.getSrcX()][move.getSrcY()] = Color.EMPTY;
        doSwap(move);
    }


    private void undoJump(DominionMoveMove move) {
        if (!isValid(move.getSrcX(), move.getSrcY()) || !isValid(move.getDstX(), move.getDstY())) {
            throw new IllegalArgumentException("Move outside the board");
        }
        if (state[move.getDstX()][move.getDstY()] != move.getColor()) {
            throw new IllegalArgumentException("Cell does not belong to the player");
        }
        if (state[move.getSrcX()][move.getSrcY()] != Color.EMPTY) {
            throw new IllegalArgumentException("Cell is not empty");
        }
        state[move.getSrcX()][move.getSrcY()] = state[move.getDstX()][move.getDstY()];
        state[move.getDstX()][move.getDstY()] = Color.EMPTY;
        undoSwap(move);
    }

    private int findNearest(int originX, int originY, int dx, int dy, Player.Color color) {
        assert dx != 0 || dy != 0;
        for (int k = 1; k < getSize(); ++k) {
            int x = originX + k * dx;
            int y = originY + k * dy;
            if (!isValid(x, y))
                return 0;
            if (state[x][y] == color)
                return k;
        }
        return 0;
    }

    private void doSwap(SwappingMove move) {
        Color opp = Player.getOpponent(move.getColor());
        for (int[] d : ADJACENT) {
            int x = move.getX() + d[0];
            int y = move.getY() + d[1];
            if (!isValid(x, y)) {
                continue;
            }
            if (state[x][y] == opp) {
                move.getSwappings().add(new int[]{x, y});
            }
            int nextOurs = findNearest(move.getX(), move.getY(), d[0], d[1], move.getColor());
            if (nextOurs >= 3) {
                boolean allEnemy = true;
                for (int k = 2; k < nextOurs; ++k)
                    if (state[move.getX() + k * d[0]][move.getY() + k * d[1]] != opp) {
                        allEnemy = false;
                        break;
                    }
                if (allEnemy) {
                    for (int k = 2; k < nextOurs; ++k)
                        move.getSwappings().add(new int[]{move.getX() + k * d[0], move.getY() + k * d[1]});
                }
            }
            for (int[] s : move.getSwappings())
                state[s[0]][s[1]] = move.getColor();
        }
    }


    private void undoSwap(SwappingMove move) {
        Color opp = Player.getOpponent(move.getColor());
        for (int[] swapping : move.getSwappings()) {
            state[swapping[0]][swapping[1]] = opp;
        }
        move.getSwappings().clear();
    }


    @Override
    public void doMove(Move move) {
        if (move instanceof DominionPlaceMove) {
            doPlace((DominionPlaceMove) move);
        } else if (move instanceof DominionMoveMove) {
            doJump((DominionMoveMove) move);
        } else if (move instanceof DominionSkipMove) {
            //do nothing
        } else {
            throw new IllegalArgumentException("Unknown type of move");
        }
    }


    @Override
    public List<Move> getMovesFor(Color color) {
        List<Move> result = new ArrayList<>();
        HashSet<Move> drops = new HashSet<>();
        for (int i = 0; i < getSize(); ++i) {
            for (int j = 0; j < getSize(); ++j) {
                if (getState(i, j) == color) {
                    addDrop(drops, color, i, j);
                    addJump(result, color, i, j);
                }
            }
        }
        result.addAll(drops);
        if (result.isEmpty()) {
            result.add(new DominionSkipMove(color));
        }
        return result;
    }


    @Override
    public void undoMove(Move move) {
        if (move instanceof DominionPlaceMove) {
            undoPlace((DominionPlaceMove) move);
        } else if (move instanceof DominionMoveMove) {
            undoJump((DominionMoveMove) move);
        } else if (move instanceof DominionSkipMove) {
            //do nothing
        } else {
            throw new IllegalArgumentException("Unknown type of move");
        }
    }


    private static final int[][] ADJACENT = new int[][]{{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}};


    /**
     * Dodaje ruchy położenia kamienia na pustych polach około kamienia (x,y)
     */
    private void addDrop(HashSet<Move> result, Color color, int x, int y) {
        assert isValid(x, y);
        assert getState(x, y) == color;
        for (int[] d : ADJACENT) {
            int i = x + d[0];
            int j = y + d[1];
            if (isValid(i, j) && getState(i, j) == Color.EMPTY) {
                result.add(new DominionPlaceMove(color, i, j));
            }
        }
    }


    private static final int[][] JUMP_RANGE = new int[][]{{-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
            {2, -1}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {-1, 2}, {-2, 2}, {-2, 1}, {-2, 0},
            {-2, -1}};


    /**
     * Dodaje ruchy skoku kamienia (x,y) na puste pole w odległości dwóch punktów kratowych
     */
    private void addJump(List<Move> result, Color color, int x, int y) {
        assert isValid(x, y);
        assert getState(x, y) == color;
        for (int[] d : JUMP_RANGE) {
            int i = x + d[0];
            int j = y + d[1];
            if (isValid(i, j) && getState(i, j) == Color.EMPTY) {
                result.add(new DominionMoveMove(color, x, y, i, j));
            }
        }
    }


    @Override
    public Color getWinner(Color currentPlayer) {
        int p1 = 0, p2 = 0;
        int n = getSize();
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (state[i][j] == Color.PLAYER1) {
                    p1++;
                } else if (state[i][j] == Color.PLAYER2) {
                    p2++;
                }
            }
        }
        if (p1 == 0)
            return Color.PLAYER2;
        if (p2 == 0)
            return Color.PLAYER1;
        if (p1 + p2 != n * n) {
            return null;
        }
        if (p1 > p2) {
            return Color.PLAYER1;
        } else if (p1 < p2) {
            return Color.PLAYER2;
        } else {
            return Color.EMPTY;
        }
    }
}
