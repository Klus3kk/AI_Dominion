package put.ai.games.cli;

import org.junit.Test;
import put.ai.games.game.Player;
import put.ai.games.game.exceptions.NoMoveException;

import static org.junit.Assert.assertEquals;

public class MessageTest {

    @Test
    public void space() {
        assertEquals("\"proper name\"", Message.escape("proper name"));
    }

    @Test
    public void eol() {
        assertEquals("\"proper_name\"", Message.escape("proper\nname"));
    }

    @Test
    public void backspace() {
        assertEquals("\"proper_name\"", Message.escape("proper\bname"));
    }

    @Test
    public void tab() {
        assertEquals("\"proper\tname\"", Message.escape("proper\tname"));
    }

    @Test
    public void quot() {
        assertEquals("\"proper\"\"name\"\"\"", Message.escape("proper\"name\""));
    }

    @Test
    public void ruleViolationException() {
        Message m = new Message();
        m.player1 = "Gracz Naiwny 84868";
        m.player2 = "Null Player 84868";
        m.winner = Player.Color.PLAYER1;
        m.exception = new NoMoveException(Player.Color.PLAYER2);
        String expected = "\"Gracz Naiwny 84868\";\"Null Player 84868\";\"PLAYER1\";\"put.ai.games.game.exceptions.NoMoveException: PLAYER2: Nie wygenerowa_ ruchu\";";
        assertEquals(expected, m.toString());
    }
}
