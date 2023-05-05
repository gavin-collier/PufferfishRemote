package com.pufferfish;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

public class connectionHandler {

    public static Socket socket;
    public Dispatcher dispatcher = new Dispatcher();

    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .readTimeout(1, TimeUnit.MINUTES) // important for HTTP long-polling
            .build();

    private IO.Options options = IO.Options.builder()
            .setForceNew(true)
            .build();

    public static ArrayList<controllerManager> players = new ArrayList<controllerManager>();
    boolean roomEmpty = false;

    public void endConnection() {
        socket.disconnect();
        dispatcher.executorService().shutdown();
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

        socket.on("newUserNotification", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                players.add(new controllerManager((String) args[0]));
            }
        });

        socket.on("buttonPressed", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Button pressed event");
                for (int i = 0; i < players.size(); i++) {
                    System.out.println("Checking player " + i);
                    System.out.println("Checking player id " + ((controllerManager) players.get(i)).getSocket());
                    System.out.println("Checking socket id " + (String) args[0]);
                    if (((controllerManager) players.get(i)).getSocket().equals((String) args[0])) {
                        try {
                            System.out.println("Sending button press to player " + i);
                            ((controllerManager) players.get(i)).pressButton((int) args[1]);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                
                }
            }
        });
        
        options.callFactory = okHttpClient;
        options.webSocketFactory = okHttpClient;
        socket.connect();
        System.out.println("Connected to server");
    }

    public boolean createRoom(String roomName) {
        socket.emit("checkRoom", roomName, (Ack) args -> {
            String response = (String) args[0];
            System.out.println("checkRoom response: " + response);
            if (response.equals("MISSING")) {
                System.out.println("Room is available");
                roomEmpty = true;
            }
        });
        if (roomEmpty) {
            socket.emit("createRoom", roomName, (Ack) args -> {
                String response = (String) args[0];
                System.out.println("Room created: " + response);
            });
        }
        return !roomEmpty;
    }
}