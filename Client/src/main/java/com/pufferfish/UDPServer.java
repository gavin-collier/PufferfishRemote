package com.pufferfish;

import io.socket.client.IO;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;

public class UDPServer {
    DatagramSocket udpSocket;
    private int serverId;
    private boolean isRunning;
    private byte[] receiveBuffer = new byte[1024];
    private controllerManager controllerManager;

    enum MessageType {
        DSUC_VersionReq(0x100000),
        DSUS_VersionRsp(0x100000),
        DSUC_ListPorts(0x100001),
        DSUS_PortInfo(0x100001),
        DSUC_PadDataReq(0x100002),
        DSUS_PadDataRsp(0x100002);

        private final int value;

        MessageType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    class PhysicalAddress {
        byte[] mac;
        byte[] ip;
        byte[] port;

        public PhysicalAddress(byte[] ip) {
            this.ip = ip;
        }

        public PhysicalAddress() {
            this.ip = null;
        }

        public byte[] getMac() {
            return mac;
        }

        public byte[] getIp() {
            return ip;
        }

        public byte[] getPort() {
            return port;
        }
    }

    class ClientRequestTimes {
        LocalDateTime allPads;
        LocalDateTime[] padIds;
        HashMap<PhysicalAddress, LocalDateTime> padMacs;

        public LocalDateTime getAllPadsTime() {
            return allPads;
        }

        public LocalDateTime[] getPadIdsTime() {
            return padIds;
        }

        public HashMap<PhysicalAddress, LocalDateTime> getPadMacsTime() {
            return padMacs;
        }

        public ClientRequestTimes() {
            allPads = LocalDateTime.MIN;
            padIds = new LocalDateTime[4];

            for (int i = 0; i < padIds.length; i++) {
                padIds[i] = LocalDateTime.MIN;
            }

            padMacs = new HashMap<PhysicalAddress, LocalDateTime>();
        }

        public void requestPadInfo(
                byte regFlags, byte idToReg, PhysicalAddress macToReg) {
            LocalDateTime currentTime = LocalDateTime.now();

            if (regFlags == 0) {
                allPads = currentTime;
            } else {
                if ((regFlags & 0x01) != 0) { // id valid
                    if (idToReg < padIds.length) {
                        padIds[idToReg] = currentTime;
                    }
                }
                if ((regFlags & 0x02) != 0) { // mac valid
                    padMacs.put(macToReg, currentTime);
                }
            }
        }
    }

    public Map<InetSocketAddress, ClientRequestTimes> clients = new HashMap<>();

    public UDPServer(controllerManager controllerManager) {
        this.controllerManager = controllerManager;
    }

    public void Start(InetAddress ip) {
        if (isRunning) {
            System.out.println("UDP Server is allready running!");
            if (udpSocket != null) {
                udpSocket.close();
                udpSocket = null;
            }
            isRunning = false;
        }

        try {
            DatagramChannel channel = DatagramChannel.open();
            udpSocket = channel.socket();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            udpSocket.bind(new InetSocketAddress(ip, 26760));
        } catch (SocketException e) {
            udpSocket.close();
            udpSocket = null;
            System.out.println("Could not start UDP Server! Error: " + e.getMessage());
            return;
        }

        byte[] randomBuf = new byte[4];
        new Random().nextBytes(randomBuf);
        serverId = ByteBuffer.wrap(randomBuf, 0, 4).get();

        isRunning = true;
        System.out.printf("Starting server on %s:%d\r\n", udpSocket.getLocalAddress(), udpSocket.getLocalPort());
        StartReceive();
    }

    public void Stop() {
        isRunning = false;
        udpSocket.close();
        udpSocket = null;
    }

    private void SendPacket(InetSocketAddress clientEP, byte[] sendBuffer, short ProtocolVersion) {
        byte[] packetData = new byte[sendBuffer.length + 16];
        packetData = BeginPacket(packetData, ProtocolVersion);
        System.arraycopy(sendBuffer, 0, packetData, 16, sendBuffer.length);
        packetData = FinishPacket(packetData);
        try {
            System.out.printf("Sending packet to %s:%d\r\n", clientEP.getAddress().toString(), clientEP.getPort());
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, clientEP.getAddress(),
                    clientEP.getPort());
            udpSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("packet send failed");
        }
    }

    private void ProcessIncomingPacket(byte[] msg, InetSocketAddress clientEP) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(msg).order(ByteOrder.nativeOrder());
            int currentIdx = 0;

            if (msg[0] != 'D' || msg[1] != 'S' || msg[2] != 'U' || msg[3] != 'C') {
                System.out.println("incorrect msg header: " + msg[0] + msg[1] + msg[2] + msg[3] + " is not valid!");
                return;
            }
            currentIdx = 4;

            int ProtocolVersion = buffer.getShort(currentIdx);
            currentIdx += 2;

            int packetSize = buffer.getShort(currentIdx);
            currentIdx += 2;

            if (packetSize < 0) {
                System.out.println("incorrect msg header!\n" + packetSize + " is not valid size!");
                return;
            }

            packetSize += 16;
            if (packetSize > msg.length) {
                System.out.println(
                        "incorrect msg header!\n" + packetSize + " is not valid size! + msg size: " + msg.length);
                return;
            } else if (packetSize < msg.length) {
                byte[] newMsg = new byte[packetSize];
                System.arraycopy(msg, 0, newMsg, 0, packetSize);
                msg = newMsg;
            }

            int crcValue = buffer.getInt(currentIdx);

            msg[currentIdx++] = 0;
            msg[currentIdx++] = 0;
            msg[currentIdx++] = 0;
            msg[currentIdx++] = 0;

            CRC32 crc32 = new CRC32();
            crc32.update(msg);
            long crcCalc = crc32.getValue();
            crc32.reset();

            if (crcCalc != crcValue) {
                System.out.println("incorrect msg header!\ncrcCalc: " + crcCalc + " != crcValue: " + crcValue + "!");
                return;
            }

            int clientID = buffer.getInt(currentIdx);
            currentIdx += 4;

            int messageType = buffer.getInt(currentIdx);
            currentIdx += 4;

            if (messageType == MessageType.DSUC_VersionReq.getValue()) {
                System.out.println("MSG TYPE: VersionReq: " + messageType);

                byte[] outputData = new byte[8];
                int outIdx = 0;

                ByteBuffer messageTypeBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(
                        MessageType.DSUS_VersionRsp.getValue());
                System.arraycopy(messageTypeBytes.array(), 0, outputData, outIdx, 4);
                outIdx += 4;

                short MaxProtocolVersion = 1001;
                ByteBuffer maxProtocolVersionBytes = ByteBuffer.allocate(2).putShort(MaxProtocolVersion)
                        .order(ByteOrder.nativeOrder());
                System.arraycopy(maxProtocolVersionBytes.array(), 0, outputData, outIdx, 2);
                outIdx += 2;

                outputData[outIdx++] = 0;
                outputData[outIdx++] = 0;

                SendPacket(clientEP, outputData, (short) 1001);

            } else if (messageType == MessageType.DSUC_ListPorts.getValue()) {
                System.out.println("MSG TYPE: ListPorts: " + messageType);

                int numPadRequest = buffer.getInt(currentIdx);
                currentIdx += 4;

                if (numPadRequest < 0 || numPadRequest > 4) {
                    System.out.println("Invalid numPadRequest #: " + numPadRequest);
                    return;
                }

                int requestIdx = currentIdx;

                for (int i = 0; i < numPadRequest; i++) {
                    byte currentRequest = msg[requestIdx + i];
                    if (currentRequest < 0 || currentRequest > 4) {
                        return;
                    }
                }

                byte[] outputData = new byte[16];

                for (byte i = 0; i < numPadRequest; i++) {
                    byte currentRequest = msg[requestIdx + i];
                    Controller controller = controllerManager.getPlayer(i);

                    if (controller == null) {
                        for (int j = 0; j < 12; j++) {
                            outputData[j] = 0;
                        }
                    } else {
                        int outIdx = 0;

                        ByteBuffer outputBuffer = ByteBuffer.wrap(outputData).order(ByteOrder.nativeOrder());
                        outputBuffer.putInt(outIdx, MessageType.DSUS_PortInfo.getValue());
                        outputData = outputBuffer.array();
                        outIdx += 4;

                        System.out.println("Sending Controller #" + currentRequest);
                        outputData[outIdx++] = currentRequest; // Slot
                        outputData[outIdx++] = (byte) controller.connected; // state
                        outputData[outIdx++] = (byte) controller.model; // gyro type
                        outputData[outIdx++] = (byte) controller.connection; // connection type

                        outIdx++;
                        byte[] addressByte = controller.PadMacAddress.mac;
                        System.arraycopy(addressByte, 0, outputData, outIdx, 6);
                        outIdx += 6;

                        outputData[outIdx++] = controller.battery;

                        for (int j = 0; j < outputData.length; j++) {
                            System.out.print(" " + outputData[j]);
                        }
                        System.out.println();
                    }

                    SendPacket(clientEP, outputData, (short) 1001);
                }
            } else if (messageType == MessageType.DSUS_PadDataRsp.getValue()) {
                System.out.println("MSG TYPE: PadDataRsp: " + messageType);

                byte regFlags = msg[currentIdx++];
                byte idToReg = msg[currentIdx++];
                PhysicalAddress macToReg = null;

                byte[] macBytes = new byte[6];
                System.arraycopy(msg, currentIdx, macBytes, 0, macBytes.length);
                currentIdx += macBytes.length;
                macToReg = new PhysicalAddress(macBytes);

                // TODO: Make sure the flags work with my remotes and are necessary
                // this is junky and I need to check to make sure it dose what I think
                // it dose
                synchronized (clients) {
                    if (clients.containsKey(clientEP)) {
                        clients.get(clientEP).requestPadInfo(regFlags, idToReg, macToReg);
                    } else {
                        ClientRequestTimes clientTimes = new ClientRequestTimes();
                        clientTimes.requestPadInfo(regFlags, idToReg, macToReg);
                        clients.put(clientEP, clientTimes);
                    }
                }
            } else {
                System.out.println("Unknown MSG Type: " + messageType);
            }

        } catch (Exception e) {
            // TODO: handle exception
            System.out.println("Can't Handle Packet! " + e.getMessage());
        }
    }

    private byte[] outputControllerState(Controller controller, byte[] outputData, int outIdx) {

        if (controller.dpad.west) {
            outputData[outIdx] |= 0x80;
        }
        if (controller.dpad.south) {
            outputData[outIdx] |= 0x40;
        }
        if (controller.dpad.east) {
            outputData[outIdx] |= 0x20;
        }
        if (controller.dpad.north) {
            outputData[outIdx] |= 0x10;
        }

        outputData[outIdx++] = 0;

        if (controller.buttons.b) {
            outputData[outIdx] |= 0x40;
        }
        if (controller.buttons.a) {
            outputData[outIdx] |= 0x20;
        }

        outputData[outIdx++] = (controller.buttons.home) ? (byte) 1 : (byte) 0;
        outputData[outIdx++] = (byte) 0; // touchpad spot

        // joysticks
        outputData[outIdx++] = 0; //left X
        outputData[outIdx++] = 0; //left y
        outputData[outIdx++] = 0; //right x
        outputData[outIdx++] = 0; //right y

        outputData[outIdx++] = (controller.dpad.west) ? (byte) 0xFF : (byte) 0; //dpad left
        outputData[outIdx++] = (controller.dpad.south) ? (byte) 0xFF : (byte) 0; //dpad down
        outputData[outIdx++] = (controller.dpad.east) ? (byte) 0xFF : (byte) 0; //dpad right
        outputData[outIdx++] = (controller.dpad.north) ? (byte) 0xFF : (byte) 0; //dpad up

        // analog buttons
        outputData[outIdx++] = (controller.buttons.one) ? (byte) 0xFF : (byte) 0;
        outputData[outIdx++] = (controller.buttons.b) ? (byte) 0xFF : (byte) 0;
        outputData[outIdx++] = (controller.buttons.a) ? (byte) 0xFF : (byte) 0;
        outputData[outIdx++] = (controller.buttons.two) ? (byte) 0xFF : (byte) 0; 

        outputData[outIdx++] = (byte) 0; //R1
        outputData[outIdx++] = (byte) 0; //L1
        outputData[outIdx++] = (byte) 0; //R2
        outputData[outIdx++] = (byte) 0; //L2

        //touch

        //first touch
        outputData[outIdx++] = (byte) 0; //is off
        outputData[outIdx++] = (byte) 0;
        outputData[outIdx++] = (byte) 0;
        outputData[outIdx++] = (byte) 0;
        outputData[outIdx++] = (byte) 0;
        outputData[outIdx++] = (byte) 0;

        //second touch
        outputData[outIdx++] = (byte) 0; //is off
        outputData[outIdx++] = (byte) 0;
        outputData[outIdx++] = (byte) 0;
        outputData[outIdx++] = (byte) 0;
        outputData[outIdx++] = (byte) 0;
        outputData[outIdx++] = (byte) 0;

        // TODO: accelerometer
        outIdx++;
        ByteBuffer accelerometerBuffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        accelerometerBuffer.putInt(0); //x
        System.arraycopy(accelerometerBuffer.array(), 0, outputData, outIdx, 4);
        accelerometerBuffer.clear();
        outIdx += 4;
        
        outIdx++;
        accelerometerBuffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        accelerometerBuffer.putInt(0); //Y
        System.arraycopy(accelerometerBuffer.array(), 0, outputData, outIdx, 4);
        accelerometerBuffer.clear();
        outIdx += 4;

        outIdx++;
        accelerometerBuffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        accelerometerBuffer.putInt(0); //Z
        System.arraycopy(accelerometerBuffer.array(), 0, outputData, outIdx, 4);
        accelerometerBuffer.clear();
        outIdx += 4;

        // TODO: gyroscope
        outIdx++;
        ByteBuffer gyroscopeBuffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        gyroscopeBuffer.putInt(0); //pitch
        System.arraycopy(gyroscopeBuffer.array(), 0, outputData, outIdx, 4);
        gyroscopeBuffer.clear();
        outIdx += 4;

        outIdx++;
        gyroscopeBuffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        gyroscopeBuffer.putInt(0); //yaw
        System.arraycopy(gyroscopeBuffer.array(), 0, outputData, outIdx, 4);
        gyroscopeBuffer.clear();
        outIdx += 4;

        outIdx++;
        gyroscopeBuffer = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder());
        gyroscopeBuffer.putInt(0); //roll
        System.arraycopy(gyroscopeBuffer.array(), 0, outputData, outIdx, 4);
        gyroscopeBuffer.clear();
        outIdx += 4;

        return outputData;
    }

    public void StateUpdate(Controller controller) {
        // TODO: run is alive check on all controlers

        byte outputData[] = new byte[100];
        int outIdx = 0;

        ByteBuffer messageTypeBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(
                MessageType.DSUS_PortInfo.getValue());
        System.arraycopy(messageTypeBytes.array(), 0, outputData, outIdx, 4);
        outIdx += 4;

        outputData[outIdx++] = (byte) controller.PadId;
        outputData[outIdx++] = (byte) controller.connected;
        outputData[outIdx++] = (byte) controller.model;
        outputData[outIdx++] = (byte) controller.connection;

        byte[] addressByte = controller.PadMacAddress.mac;
        System.arraycopy(addressByte, 0, outputData, outIdx, 6);
        outIdx += 6;

        outputData[outIdx++] = (byte) controller.battery;
        outputData[outIdx++] = 1;

        messageTypeBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(controller.packageCounter++);
        System.arraycopy(messageTypeBytes.array(), 0, outputData, outIdx, 4);
        outIdx += 4;

        outputData = outputControllerState(controller, outputData, outIdx);

        InetSocketAddress clientEP = new InetSocketAddress(udpSocket.getLocalAddress(), udpSocket.getPort());
        SendPacket(clientEP, outputData, (short) 1001);
    }

    private void ReceiveCallback(DatagramPacket packet) {
        byte[] msg = null;
        InetSocketAddress clientEP = new InetSocketAddress(udpSocket.getLocalAddress(), udpSocket.getPort());
        try {
            int msgLen = packet.getLength();
            msg = new byte[msgLen];
            System.arraycopy(receiveBuffer, 0, msg, 0, msgLen);
        } catch (Exception e) {
            System.out.println("Callback problem: " + e.getMessage());
            for (int i = 0; i < receiveBuffer.length; i++) {
                System.out.print(" " + receiveBuffer[i]);
            }
        }
        if (msg != null) {
            ProcessIncomingPacket(msg, clientEP);
        }

        StartReceive();
    }

    private void StartReceive() {
        try {
            InetAddress localHostEP = InetAddress.getLoopbackAddress();
            DatagramPacket packet = new DatagramPacket(
                    receiveBuffer, receiveBuffer.length, localHostEP, 26760);
            udpSocket.receive(packet);
            udpSocket.connect(localHostEP, packet.getPort());
            ReceiveCallback(packet);
        } catch (IOException e) {
            System.out.println("Failed to StartReceive: " + e.getMessage());
            try {
                udpSocket.setOption(StandardSocketOptions.SO_BROADCAST, false);
                udpSocket.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            StartReceive();
        }
    }

    private byte[] BeginPacket(byte[] packetBuffer, short ProtocalVersion) {
        int currentIdx = 0;
        packetBuffer[currentIdx++] = (byte) 'D';
        packetBuffer[currentIdx++] = (byte) 'S';
        packetBuffer[currentIdx++] = (byte) 'U';
        packetBuffer[currentIdx++] = (byte) 'S';

        ByteBuffer reqProtocolVersionBytes = ByteBuffer.allocate(2).order(ByteOrder.nativeOrder())
                .putShort(ProtocalVersion);
        System.arraycopy(
                reqProtocolVersionBytes.array(), 0, packetBuffer, currentIdx, 2);
        currentIdx += 2;

        ByteBuffer packetLengthBytes = ByteBuffer.allocate(2).order(ByteOrder.nativeOrder())
                .putShort((short) (packetBuffer.length - 16));
        System.arraycopy(
                packetLengthBytes.array(), 0, packetBuffer, currentIdx, 2);
        currentIdx += 2;

        Arrays.fill(packetBuffer, currentIdx, currentIdx + 4, (byte) 0);
        currentIdx += 4;

        ByteBuffer serverIdBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(serverId);
        System.arraycopy(serverIdBytes.array(), 0, packetBuffer, currentIdx, 4);
        currentIdx += 4;

        return packetBuffer;
    }

    private byte[] FinishPacket(byte[] packetBuffer) {
        Arrays.fill(packetBuffer, 8, 12, (byte) 0);

        CRC32 crc32 = new CRC32();
        crc32.update(packetBuffer);
        long crcCalc = crc32.getValue();

        ByteBuffer crcBytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt((int) crcCalc);
        System.arraycopy(crcBytes.array(), 0, packetBuffer, 8, 4);

        return packetBuffer;
    }
}