package com.pufferfish;

import java.io.IOException;

import io.socket.client.Socket;

public class controllerManager {
    private static Socket socket;
    public static int playerNumber;
    Controller controller;

    public controllerManager(Socket socket, int playerNumber) {
        this.socket = socket;
        this.playerNumber = playerNumber;
        try {
            controller  = new Controller();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pressButton(int button) throws IOException{
        controller.sendButton(button);
    }

    public Socket getSocket() {
        return socket;
    }
}