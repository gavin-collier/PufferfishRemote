package com.pufferfish;

import java.io.IOException;

public class controllerManager {
    private String id;
    public static int playerNumber;
    Controller controller;

    public controllerManager(String id) {
        this.id = id;
        playerNumber++;
        controller = new Controller();
    }

    public void pressButton(int button) throws IOException{
        System.out.println("Sending button by " + playerNumber + " button: " + button);
    }

    public String getSocket() {
        return id;
    }
}