package com.pufferfish;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

class UpdateObjects implements Runnable {
    public void run() {

    }
}

public class FXMLController implements Initializable {

    String roomCode;

    public static controllerManager controllerManager = new controllerManager();
    static UDPServer udpServer = new UDPServer(controllerManager);
    static connectionHandler connectionHandler = new connectionHandler(controllerManager, udpServer);

    @FXML
    public static ListView<String> playerList;

    @FXML
    private Label roomCodeLabel, roomCodeTextLabel;

    @FXML
    synchronized private void handleButtonAction(ActionEvent event) throws InterruptedException {
        roomCode = generateRandomString(6);

        while (connectionHandler.createRoom(roomCode)) {
            roomCode = generateRandomString(6);
            System.out.println("Room Code: " + roomCode);
            wait(500);
        }

        roomCodeLabel.setDisable(false);
        roomCodeTextLabel.setDisable(false); 
        roomCodeLabel.setText(roomCode);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            connectionHandler.newConnection(new URI("ws://localhost:8080")); // https://pufferfish.onrender.com
            try {
                System.out.println("Tried to Start UDP Server!");
                
                Thread asyncThread = new Thread(() -> {
                    udpServer.Start(InetAddress.getLoopbackAddress());
                });
        
                asyncThread.start();
            } catch (Exception e) {
                System.out.println("Failed UDP Start: " + e.getMessage());
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        new Thread(new UpdateObjects()).start();
    }

    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static void endConnections() {
        connectionHandler.endConnection();
    }
}
