package put.ai.games.dominion;

import put.ai.games.engine.UniversalBoardFactory;

public class DominionBoardFactory extends UniversalBoardFactory {

    public DominionBoardFactory()
            throws NoSuchMethodException {
        super(DominionBoard.class, "Dominion", "https://jpneto.github.io/world_abstract_games/dominion.htm");
    }
}