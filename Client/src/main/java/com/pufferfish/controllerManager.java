package com.pufferfish;

import java.util.ArrayList;

public class controllerManager {
    public static ArrayList<Controller> players = new ArrayList<Controller>();

    public controllerManager() {
    }

    public void addPlayer(int index, Controller player) {
        if (index != -1) {
            players.add(index, player);
        } else {
            players.add(player);
        }
    }

    public Controller getPlayer(int playerNum){
        if (playerNum > players.size() - 1){
            return null;
        }
        return players.get(playerNum);
    }

    public String getSocket(int playerNum) {
        return players.get(playerNum).getSocket();
    }
}