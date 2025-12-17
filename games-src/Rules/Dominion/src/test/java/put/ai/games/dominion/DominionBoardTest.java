/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.dominion;

import org.junit.Before;
import org.junit.Test;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author smaug
 */
public class DominionBoardTest {

    private DominionBoard b;


    @Before
    public void before() {
        b = new DominionBoard(8);
    }


    @Test
    public void fullBoard1() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1,
                        Player.Color.PLAYER1},
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1,
                        Player.Color.PLAYER1},
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER2,
                        Player.Color.PLAYER2},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2,
                        Player.Color.PLAYER2},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2,
                        Player.Color.PLAYER2},};
        b.setState(state);
        assertEquals(Player.Color.PLAYER1, b.getWinner(Player.Color.EMPTY));
    }


    @Test
    public void fullBoard2() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1,
                        Player.Color.PLAYER1},
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1,
                        Player.Color.PLAYER1},
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER2, Player.Color.PLAYER2,
                        Player.Color.PLAYER2},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2,
                        Player.Color.PLAYER2},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2,
                        Player.Color.PLAYER2},};
        b.setState(state);
        assertEquals(Player.Color.PLAYER2, b.getWinner(Player.Color.EMPTY));
    }


    @Test
    public void fullBoard3() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1},
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2},};
        b.setState(state);
        assertEquals(Player.Color.EMPTY, b.getWinner(Player.Color.EMPTY));
    }


    @Test
    public void notFullBoard() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1},
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.EMPTY},};
        b.setState(state);
        assertEquals(null, b.getWinner(Player.Color.EMPTY));
    }

    @Test
    public void only1() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1},
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.PLAYER1},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},};
        b.setState(state);
        assertEquals(Player.Color.PLAYER1, b.getWinner(Player.Color.EMPTY));
    }

    @Test
    public void only2() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2},
                {Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2, Player.Color.PLAYER2},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},};
        b.setState(state);
        assertEquals(Player.Color.PLAYER2, b.getWinner(Player.Color.EMPTY));
    }

    private void assertState(Player.Color[][] expected) {
        for (int x = 0; x < b.getSize(); ++x)
            for (int y = 0; y < b.getSize(); ++y)
                assertEquals(expected[x][y], b.getState(x, y));
    }


    @Test
    public void verticalSandwich() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.PLAYER2, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.PLAYER2, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},};
        Player.Color[][] expected = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY},};
        b.setState(state);
        Optional<Move> move = b.getMovesFor(Player.Color.PLAYER1).stream().filter(m -> m instanceof DominionPlaceMove && ((DominionPlaceMove) m).getX() == 3 && ((DominionPlaceMove) m).getY() == 1).findFirst();
        assertTrue(move.isPresent());
        b.doMove(move.get());
        assertState(expected);
        assertEquals(Player.Color.PLAYER1, b.getWinner(Player.Color.EMPTY));
    }

    @Test
    public void diagonalSandwich() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER2, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER2, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY}
                ,};
        Player.Color[][] expected = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY}
                ,};
        b.setState(state);
        Optional<Move> move = b.getMovesFor(Player.Color.PLAYER1).stream().filter(m -> m instanceof DominionPlaceMove && ((DominionPlaceMove) m).getX() == 4 && ((DominionPlaceMove) m).getY() == 1).findFirst();
        assertTrue(move.isPresent());
        b.doMove(move.get());
        assertState(expected);
        assertEquals(Player.Color.PLAYER1, b.getWinner(Player.Color.EMPTY));
    }

    @Test
    public void diagonalNotSandwich() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER2, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY}
                ,};
        Player.Color[][] expected = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY}
                ,};
        b.setState(state);
        Optional<Move> move = b.getMovesFor(Player.Color.PLAYER1).stream().filter(m -> m instanceof DominionPlaceMove && ((DominionPlaceMove) m).getX() == 4 && ((DominionPlaceMove) m).getY() == 1).findFirst();
        assertTrue(move.isPresent());
        b.doMove(move.get());
        assertState(expected);
        assertEquals(Player.Color.PLAYER1, b.getWinner(Player.Color.EMPTY));
    }

    @Test
    public void diagonalSandwichJump() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY},
                {Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER2, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER2, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY}
                ,};
        Player.Color[][] expected = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY}
                ,};
        b.setState(state);
        Optional<Move> move = b.getMovesFor(Player.Color.PLAYER1).stream().filter(m -> m instanceof DominionMoveMove && ((DominionMoveMove) m).getX() == 4 && ((DominionMoveMove) m).getY() == 1).findFirst();
        assertTrue(move.isPresent());
        b.doMove(move.get());
        assertState(expected);
        assertEquals(Player.Color.PLAYER1, b.getWinner(Player.Color.EMPTY));
    }

    @Test
    public void noDuplicatedDrops() {
        Player.Color[][] state = new Player.Color[][]{
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.PLAYER1, Player.Color.EMPTY, Player.Color.PLAYER1},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},
                {Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY, Player.Color.EMPTY},};
        b.setState(state);
        List<Move> dropMoves = b.getMovesFor(Player.Color.PLAYER1).stream().filter(m -> m instanceof DominionPlaceMove).toList();
        assertEquals(10, dropMoves.size());
        long n22 = dropMoves.stream().filter(m -> ((DominionPlaceMove) m).getX() == 2 && ((DominionPlaceMove) m).getY() == 2).count();
        assertEquals(1, n22);
    }
}
