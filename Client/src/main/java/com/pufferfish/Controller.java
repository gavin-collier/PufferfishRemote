package com.pufferfish;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Controller {

    private static final int PORT = 26760;
    private static final int PACKET_SIZE = 1024;

    private DatagramSocket socket;
    private InetAddress address;

    public Controller() throws IOException {
        socket = new DatagramSocket(PORT);
        address = InetAddress.getLocalHost();
    }

    public void sendButton(int button) throws IOException {
        byte[] data = new byte[PACKET_SIZE];
        data[0] = (byte) button;
        socket.send(new DatagramPacket(data, data.length, address, PORT));
    }

    public void sendAxis(int axis, float value) throws IOException {
        byte[] data = new byte[PACKET_SIZE];
        data[0] = (byte) axis;
        byte[] valueBytes = Float.toString(value).getBytes();
        System.arraycopy(valueBytes, 0, data, 1, valueBytes.length);
        socket.send(new DatagramPacket(data, data.length, address, PORT));
    }

    public void close() throws IOException {
        socket.close();
    }
}