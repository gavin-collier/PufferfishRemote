package com.pufferfish;

import java.net.URI;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class connectionHandler {
    private IO.Options options = IO.Options.builder()
            .setForceNew(true)
            .build();

    public static Socket socket;

    public void endConnection() {
        socket.disconnect();
        System.out.println("Connection Closed\nGood Bye...");
        System.exit(0);
    }

    public void newConnection(URI uri) {
        socket = IO.socket(uri, options);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                System.out.println("Connected to server");
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... arg0) {
                System.out.println("Disconnected from server");
            }

        });
        socket.connect();
        System.out.println("Connected to server");
    }
}