/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.engine.impl;

import put.ai.games.engine.BoardFactory;
import put.ai.games.engine.Callback;
import put.ai.games.engine.GameEngine;
import put.ai.games.engine.impl.MoveJobs.NextMoveJob;
import put.ai.games.engine.impl.MoveJobs.NextMoveJobForMyPlayer;
import put.ai.games.engine.impl.MoveJobs.NextMoveJobForProcess;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;
import put.ai.games.game.Player.Color;
import put.ai.games.game.exceptions.CheatingException;
import put.ai.games.game.exceptions.NoMoveException;
import put.ai.games.game.exceptions.RuleViolationException;
import put.ai.games.game.exceptions.TimeoutException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameEngineImpl implements GameEngine {

    private ObjectOutputStream[] outputs = new ObjectOutputStream[2];
    private ObjectInputStream[] inputs = new ObjectInputStream[2];
    private Color[] colors = new Color[]{Color.PLAYER1, Color.PLAYER2};
    private int timeout = 20000; //ms
    private BoardFactory boardFactory;
    private Player[] myPlayers = new Player[2];
    private boolean[] isMyPlayer = {false, false};
    private boolean[] isPlayerLoaded = {false, false};
    private String[] names = new String[2];


    public GameEngineImpl(BoardFactory boardFactory) {
        this.boardFactory = boardFactory;
    }


    @Override
    public String addPlayer(Process p) throws IOException {
        for (int i = 0; i < outputs.length; ++i) {
            if (!isPlayerLoaded[i]) {
                isPlayerLoaded[i] = true;
                outputs[i] = new ObjectOutputStream(p.getOutputStream());
                outputs[i].flush();
                inputs[i] = new ObjectInputStream(p.getInputStream());
                try {
                    names[i] = (String) inputs[i].readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return names[i];
            }
        }
        throw new IllegalStateException("Maxium number of players already reached");
    }

    @Override
    public void addPlayer(Player p, int color) {
        for (int i = 0; i < outputs.length; ++i) {
            if (!isPlayerLoaded[i]) {
                isPlayerLoaded[i] = true;
                isMyPlayer[i] = true;
                myPlayers[i] = p;
                names[i] = myPlayers[i].getName();
                myPlayers[i].setColor(colors[color]);
                myPlayers[i].setTime(timeout);
                return;
            }
        }
        throw new IllegalStateException("Maxium number of players already reached");
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    private int getMaxNoOfMoves(Board b) {
        return 2 * b.getSize() * b.getSize();
    }

    @Override
    public String[] getNames() {
        return names;
    }

    @Override
    public Color play(Callback cb)
            throws RuleViolationException {
        Board b = boardFactory.create();
        if (cb != null) {
            cb.update(colors[0], b, null);
        }
        NextMoveJob job;
        for (int moveNo = 0; moveNo < getMaxNoOfMoves(b); moveNo++) {
            for (int i = 0; i < outputs.length; ++i) {
                if (isMyPlayer[i]) {
                    job = new NextMoveJobForMyPlayer(myPlayers[i], b);
                } else {
                    job = new NextMoveJobForProcess(outputs[i], inputs[i], b);
                }
                Thread t = new Thread(job);
                t.start();
                try {
                    t.join(timeout);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GameEngineImpl.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
                if (job.hasException()) {
                    Logger.getLogger(GameEngineImpl.class.getName()).log(Level.SEVERE, null, job.getException());
                    throw new RuleViolationException(colors[i], job.getException());
                }
                if (t.isAlive()) {
                    throw new TimeoutException(colors[i]);
                }
                Move m = job.getMove();
                if (m == null) {
                    throw new NoMoveException(colors[i]);
                }
                if (!MoveValidator.isValidMove(m, b, colors[i])) {
                    throw new CheatingException(colors[i]);
                }
                try {
                    b.doMove(m);
                } catch (Exception e) {
                    Logger.getLogger(GameEngineImpl.class.getName()).log(Level.SEVERE, null, e);
                    throw new RuleViolationException(colors[i], e);
                }
                if (cb != null) {
                    cb.update(colors[1 - i], b, m);
                }
                Color winner = b.getWinner(colors[1 - i]);
                if (winner != null) {
                    return winner;
                }
            }
        }
        return null;
    }
}
