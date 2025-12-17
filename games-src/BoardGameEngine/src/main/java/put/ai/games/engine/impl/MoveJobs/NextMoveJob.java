
package put.ai.games.engine.impl.MoveJobs;

import put.ai.games.game.Board;
import put.ai.games.game.Move;

public abstract class NextMoveJob implements Runnable {

    protected Board b;
    protected Move m;
    protected Exception ex = null;

    protected NextMoveJob(Board b) {
        this.b = b.clone();
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (Exception ex) {
            this.ex = ex;
        }
    }

    protected abstract void execute() throws Exception;

    public Exception getException() {
        return ex;
    }

    public boolean hasException() {
        return ex != null;
    }

    public Move getMove() {
        return m;
    }
}
