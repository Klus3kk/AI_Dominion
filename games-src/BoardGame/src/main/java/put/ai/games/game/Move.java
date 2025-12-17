/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.game;

import java.io.Serializable;

/**
 * Opis ruchu polegającego na położeniu piona
 */
public interface Move extends Serializable {

    /**
     * Kolor piona do położenia
     */
    public Player.Color getColor();
}
