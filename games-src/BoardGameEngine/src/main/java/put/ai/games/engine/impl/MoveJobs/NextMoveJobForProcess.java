
package put.ai.games.engine.impl.MoveJobs;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import put.ai.games.game.Board;
import put.ai.games.game.Move;

public class NextMoveJobForProcess extends NextMoveJob {

    private ObjectOutputStream w;
    private ObjectInputStream r;

    public NextMoveJobForProcess(ObjectOutputStream w, ObjectInputStream r, Board b) {
        super(b);
        this.w = w;
        this.r = r;
    }

    @Override
    protected void execute() throws Exception {
        this.w.writeObject(b);
        this.w.flush();
        this.m = (Move) this.r.readObject();
    }
}