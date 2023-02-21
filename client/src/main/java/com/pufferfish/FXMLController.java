package com.pufferfish;

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
import javafx.stage.Stage;

public class FXMLController implements Initializable {

    String roomCode;
    static connectionHandler connectionHandler = new connectionHandler();

    @FXML
    private ListView<String> playerList;

    @FXML
    private Label roomCodeLabel, roomCodeTextLabel;

    @FXML
    private void handleButtonAction(ActionEvent event) {
        roomCodeLabel.setDisable(false);
        roomCodeTextLabel.setDisable(false);

        roomCode = generateRandomString(6);
        roomCodeLabel.setText(roomCode);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            connectionHandler.newConnection(new URI("ws://localhost:8080"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
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
