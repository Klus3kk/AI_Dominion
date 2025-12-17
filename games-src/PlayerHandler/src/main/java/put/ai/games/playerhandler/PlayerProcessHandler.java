
package put.ai.games.playerhandler;

import put.ai.games.engine.loaders.MetaPlayerLoader;
import put.ai.games.engine.loaders.PlayerLoadingException;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;
import put.ai.games.game.Player.Color;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

public class PlayerProcessHandler {

    private static Player player;

    public static void main(String[] args) {
        OutputStream realStdOut = System.out;
        System.setOut(System.err);
        try {
            player = MetaPlayerLoader.INSTANCE.load(args[0]).getDeclaredConstructor().newInstance();
        } catch (PlayerLoadingException ex) {
            System.err.printf("Can not load player from %s: %s\n", args[0], ex);
            return;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            System.err.printf("Error instantiating player from %s: %s\n", args[0], ex);
            return;
        }
        player.setTime(Integer.parseInt(args[1]));
        if ("0".equals(args[2])) {
            player.setColor(Color.PLAYER1);
        } else {
            player.setColor(Color.PLAYER2);
        }

        try {
            ObjectInputStream inputStream = new ObjectInputStream(System.in);
            ObjectOutputStream outputStream = new ObjectOutputStream(realStdOut);
            outputStream.writeObject(player.getName());

            while (true) {
                try {
                    // Read the board object sent by the game engine
                    Board board = (Board) inputStream.readObject();

                    // Ask the player to make a move
                    Move move = player.nextMove(board);

                    // Send the move back to the game engine
                    outputStream.writeObject(move);
                    outputStream.flush();
                } catch (ClassNotFoundException ex) {
                    System.err.println("Error reading board from input stream: " + ex);
                } catch (IOException ex) {
                    System.err.println("I/O error occurred: " + ex);
                    break;
                }
            }
        } catch (IOException ex) {
            System.err.println("Error initializing streams: " + ex);
        }
    }
}
