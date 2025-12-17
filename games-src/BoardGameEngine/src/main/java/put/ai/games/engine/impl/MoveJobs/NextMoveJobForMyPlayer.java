
package put.ai.games.engine.impl.MoveJobs;

import put.ai.games.game.Board;
import put.ai.games.game.Player;

public class NextMoveJobForMyPlayer extends NextMoveJob {

    private Player p;

    public NextMoveJobForMyPlayer(Player p, Board b) {
        super(b);
        this.p = p;
    }

    @Override
    protected void execute() throws Exception {
        this.m = p.nextMove(b);
    }
}
