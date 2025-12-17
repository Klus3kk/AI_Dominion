from frontend.app import detect_public_class


def test_normal():
    cls = detect_public_class("""/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.naiveplayer;

import java.util.List;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class NaivePlayer extends Player {

    private Random random = new Random(0xdeadbeef);


    @Override
    public String getName() {
        return "Gracz Naiwny 84868";
    }


    @Override
    public Move nextMove(Board b) {
        List<Move> moves = b.getMovesFor(getColor());
        return moves.get(random.nextInt(moves.size()));
    }
}
""")
    assert cls is not None
    assert cls.package == "put.ai.games.naiveplayer"
    assert cls.name == "NaivePlayer"


def test_no_package():
    cls = detect_public_class("""
import java.util.List;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class NaivePlayer extends Player {

    private Random random = new Random(0xdeadbeef);


    @Override
    public String getName() {
        return "Gracz Naiwny 84868";
    }


    @Override
    public Move nextMove(Board b) {
        List<Move> moves = b.getMovesFor(getColor());
        return moves.get(random.nextInt(moves.size()));
    }
}
""")
    assert cls is None


def test_invalid_class_name():
    cls = detect_public_class("""
    package put.ai.games.naiveplayer;
import java.util.List;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class `naive player` extends Player {

    private Random random = new Random(0xdeadbeef);


    @Override
    public String getName() {
        return "Gracz Naiwny 84868";
    }


    @Override
    public Move nextMove(Board b) {
        List<Move> moves = b.getMovesFor(getColor());
        return moves.get(random.nextInt(moves.size()));
    }
}
""")
    assert cls is None
