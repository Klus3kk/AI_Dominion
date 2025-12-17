package put.ai.games.cli;

import put.ai.games.game.Player;

public class Message {

    static String escape(Object o) {
        if (o == null)
            return "";
        String n = o.toString();
        n = n.replaceAll("[^\\p{Graph}\\p{Blank}]", "_");
        return "\"" + n.replace("\"", "\"\"") + "\"";
    }

    String player1 = null;
    String player2 = null;
    Player.Color winner = null;
    Exception exception = null;

    public void setPlayer(int n, String name) {
        if (n == 0)
            player1 = name;
        else if (n == 1)
            player2 = name;
        else throw new IllegalArgumentException("n must be equal to 0 or 1");
    }

    @Override
    public String toString() {
        return String.join(";", escape(player1), escape(player2), escape(winner), escape(exception)) + ";";
    }
}