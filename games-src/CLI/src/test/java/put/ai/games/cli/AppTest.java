package put.ai.games.cli;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class AppTest {
    private interface Block {
        void run();
    }

    private static String captureStdout(Block block) {
        ByteArrayOutputStream base = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(base));
        try {
            block.run();
        } finally {
            System.out.flush();
            System.setOut(original);
        }
        return base.toString();
    }

    private String naivePlayer = "../Players/NaivePlayer/target/NaivePlayer-2.0-SNAPSHOT.jar";
    private String nullPlayer = "../Players/NullPlayer/target/NullPlayer-2.0-SNAPSHOT.jar";
    private String ab1 = "../Players/AlphaBetaPlayer/target/AlphaBetaPlayer-2.0-SNAPSHOT-AB1.jar";
    private App app;

    @Before
    public void setup() {
        app = new App();
        app.setPlayerHandlerPath("../PlayerHandler/target/PlayerHandler-2.0-SNAPSHOT.jar");
        Assume.assumeTrue(new File(app.getPlayerHandlerPath()).exists());
    }

    @Test
    public void naive_vs_null() {
        String actual = captureStdout(() -> app.entrypoint(new String[]{naivePlayer, nullPlayer, "Pentago"}));
        String expected = "\"Gracz Naiwny 84868\";\"Null Player 84868\";\"PLAYER1\";\"put.ai.games.game.exceptions.NoMoveException: PLAYER2: Nie wygenerowa_ ruchu\";\n";
        assertEquals(expected, actual);
    }

    @Test
    public void null_vs_naive() {
        String actual = captureStdout(() -> app.entrypoint(new String[]{nullPlayer, naivePlayer, "Pentago"}));
        String expected = "\"Null Player 84868\";\"Gracz Naiwny 84868\";\"PLAYER2\";\"put.ai.games.game.exceptions.NoMoveException: PLAYER1: Nie wygenerowa_ ruchu\";\n";
        assertEquals(expected, actual);
    }

    @Test
    public void naive_vs_naive() {
        String actual = captureStdout(() -> app.entrypoint(new String[]{naivePlayer, naivePlayer, "Pentago"}));
        String expected = "^\"Gracz Naiwny 84868\";\"Gracz Naiwny 84868\";\"PLAYER[12]\";;\n$";
        assertTrue(Pattern.matches(expected, actual));
    }

    @Test
    public void naive_vs_missing() throws IOException {
        File emptyDir = Files.createTempDirectory("games").toFile();
        try {
            String missingPlayer = new File(emptyDir, "player.jar").getAbsolutePath();
            assertFalse(new File(missingPlayer).exists());
            String actual = captureStdout(() -> app.entrypoint(new String[]{naivePlayer, missingPlayer, "Pentago"}));
            System.out.println(actual);
            assertTrue(actual.startsWith("\"Gracz Naiwny 84868\";;;\"java.lang.IllegalArgumentException:"));
        } finally {
            FileUtils.deleteDirectory(emptyDir);
        }
    }

    @Test
    public void ab1_vs_ab1() {
        File f = new File(ab1);
        Assume.assumeTrue(f.exists());
        Assume.assumeTrue(f.isFile());
        Assume.assumeTrue(f.canRead());
        String actual = captureStdout(() -> app.entrypoint(new String[]{ab1, ab1, "Kassle", "--board-size", "5", "--timeout", "2000"}));
        String expected = "^\"Gracz heurystyczny\";\"Gracz heurystyczny\";\"PLAYER[12]\";;\n$";
        assertTrue(Pattern.matches(expected, actual));
    }

    @Test
    public void noOptions() throws ParseException {
        // not using this.app to avoid the default configuration
        App app = new App();
        app.configureFromCommandLine(new String[]{nullPlayer, ab1, "Kassle"});
        assertEquals(nullPlayer, app.getPlayer1jar());
        assertEquals(ab1, app.getPlayer2jar());
        assertEquals("Kassle", app.getGame());
        assertNull(app.getBoardSize());
        assertEquals(App.DEFAULT_TIMEOUT_MS, app.getTimeout());
        assertEquals(App.DEFAULT_MEMORY_MB, app.getMemory());
        assertEquals(App.DEFAULT_PLAYER_HANDLER_PATH, app.getPlayerHandlerPath());
    }

    @Test
    public void boardSize() throws ParseException {
        app.configureFromCommandLine(new String[]{nullPlayer, ab1, "Kassle", "--board-size", "17"});
        assertEquals(nullPlayer, app.getPlayer1jar());
        assertEquals(ab1, app.getPlayer2jar());
        assertEquals("Kassle", app.getGame());
        assertEquals(Integer.valueOf(17), app.getBoardSize());
    }

    @Test
    public void memory() throws ParseException {
        app.configureFromCommandLine(new String[]{nullPlayer, ab1, "Kassle", "--memory", "16384"});
        assertEquals(nullPlayer, app.getPlayer1jar());
        assertEquals(ab1, app.getPlayer2jar());
        assertEquals("Kassle", app.getGame());
        assertEquals(16384, app.getMemory());
    }

    @Test
    public void timeout() throws ParseException {
        app.configureFromCommandLine(new String[]{nullPlayer, ab1, "Kassle", "--timeout", "16384"});
        assertEquals(nullPlayer, app.getPlayer1jar());
        assertEquals(ab1, app.getPlayer2jar());
        assertEquals("Kassle", app.getGame());
        assertEquals(16384, app.getTimeout());
    }

    @Test
    public void handler() throws ParseException {
        app.configureFromCommandLine(new String[]{nullPlayer, ab1, "Kassle", "--player-handler", "/dev/null"});
        assertEquals(nullPlayer, app.getPlayer1jar());
        assertEquals(ab1, app.getPlayer2jar());
        assertEquals("Kassle", app.getGame());
        assertEquals("/dev/null", app.getPlayerHandlerPath());
    }
}
