package com.pufferfish;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

public class UpdServer {

    private Socket udpSocket;
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

        public void requestPadInfo(byte regFlags, byte idToReg, PhysicalAddress macToReg) {
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

    public UpdServer(controllerManager controllerManager) {
        this.controllerManager = controllerManager;
    }

    private void SendPacket(InetSocketAddress clientEP, byte[] sendBuffer, short ProtocallVersion) {
        byte[] packetData = new byte[sendBuffer.length + 16];
        int currIdx = BeginPacket(packetData, ProtocallVersion);
        System.arraycopy(sendBuffer, 0, packetData, currIdx, sendBuffer.length);
        FinishPacket(packetData);

        try {
            DatagramSocket udpSock = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, clientEP.getAddress(),
                    clientEP.getPort());
            udpSock.send(packet);
            udpSock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ProcessIncomingPacket(byte[] msg, InetSocketAddress clientEP) {
        try {
            int currentIdx = 0;
            if (msg[0] != 'D' || msg[1] != 'S' || msg[2] != 'U' || msg[3] != 'C') {
                return;
            } else {
                currentIdx += 4;
            }

            int ProtocallVersion = ByteBuffer.wrap(msg, currentIdx, 2).getShort();
            currentIdx += 2;

            int packetSize = ByteBuffer.wrap(msg, currentIdx, 2).getShort();
            currentIdx += 2;

            if (packetSize < 0) {
                return;
            }

            packetSize += 16;
            if (packetSize > msg.length) {
                return;
            } else if (packetSize < msg.length) {
                byte[] newMsg = new byte[packetSize];
                System.arraycopy(msg, 0, newMsg, 0, packetSize);
                msg = newMsg;
            }

            int crcValue = ByteBuffer.wrap(msg, currentIdx, 4).getInt();
            currentIdx += 4;
            msg[currentIdx++] = (byte) 0;
            msg[currentIdx++] = (byte) 0;
            msg[currentIdx++] = (byte) 0;
            msg[currentIdx++] = (byte) 0;

            CRC32 crc32 = new CRC32();
            crc32.update(msg);
            long crcCalc = crc32.getValue();

            if (crcCalc != crcValue) {
                return;
            }

            int clientID = ByteBuffer.wrap(msg, currentIdx, 4).getInt();
            currentIdx += 4;

            int messageType = ByteBuffer.wrap(msg, currentIdx, 4).getInt();
            currentIdx += 4;

            if (messageType == MessageType.DSUC_VersionReq.getValue()) {
                byte[] outputData = new byte[8];
                int outIdx = 0;
                ByteBuffer messageTypeBytes = ByteBuffer.allocate(4).putInt(MessageType.DSUS_VersionRsp.getValue());
                System.arraycopy(messageTypeBytes.array(), 0, outputData, outIdx, 4);
                outIdx += 4;
                short MaxProtocolVersion = 1001;
                ByteBuffer maxProtocolVersionBytes = ByteBuffer.allocate(2).putShort(MaxProtocolVersion);
                System.arraycopy(maxProtocolVersionBytes.array(), 0, outputData, outIdx, 2);

                outIdx += 2;
                outputData[outIdx++] = 0;
                outputData[outIdx++] = 0;

                SendPacket(clientEP, outputData, (short) 1001);
            } else if (messageType == MessageType.DSUC_ListPorts.getValue()) {
                int numPadRequest = ByteBuffer.wrap(msg, currentIdx, 4).getInt();
                currentIdx += 4;
                if (numPadRequest < 0 || numPadRequest > 4) {
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

                    int outIdx = 0;

                    ByteBuffer messageTypeBytes = ByteBuffer.allocate(4).putInt(MessageType.DSUS_VersionRsp.getValue());
                    System.arraycopy(messageTypeBytes.array(), 0, outputData, outIdx, 4);
                    outIdx += 4;

                    outputData[outIdx++] = (byte) controller.PadId;
                    outputData[outIdx++] = (byte) controller.constants;
                    outputData[outIdx++] = (byte) controller.model;
                    outputData[outIdx++] = (byte) controller.connection;

                    byte[] adressByte = controller.PadMacAdress;

                    if (adressByte.length == 6) {
                        for (int j = 0; j < 6; j++) {
                            outputData[outIdx++] = adressByte[j];
                        }
                    } else {
                        for (int j = 0; j < 6; j++) {
                            outputData[outIdx++] = 0;
                        }
                    }

                    outputData[outIdx++] = (byte) controller.battery;
                    outputData[outIdx++] = 0;

                    SendPacket(clientEP, outputData, (short) 1001);
                }
            } else if (messageType == MessageType.DSUS_PadDataRsp.getValue()) {
                byte regFlags = msg[currentIdx++];
                byte idToReg = msg[currentIdx++];
                PhysicalAddress macToReg = null;

                byte[] macBytes = new byte[6];
                System.arraycopy(msg, currentIdx, macBytes, 0, macBytes.length);
                currentIdx += macBytes.length;
                macToReg = new PhysicalAddress(macBytes);

                synchronized (clients) {
                    if (clients.containsKey(clientEP)) {
                        clients.get(clientEP).requestPadInfo(regFlags, idToReg, macToReg);
                    } else {
                        ClientRequestTimes clientTimes = new ClientRequestTimes();
                        clientTimes.requestPadInfo(regFlags, idToReg, macToReg);
                        clients.put(clientEP, clientTimes);
                    }
                }

            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private void ReceiveCallbakc(AsyncResult ar){
        

    }

    private int BeginPacket(byte[] packetBuffer, short ProtocallVersion) {
        int currentIdx = 0;
        packetBuffer[currentIdx++] = (byte) 'D';
        packetBuffer[currentIdx++] = (byte) 'S';
        packetBuffer[currentIdx++] = (byte) 'U';
        packetBuffer[currentIdx++] = (byte) 'S';

        ByteBuffer reqProtocolVersionBytes = ByteBuffer.allocate(2).putShort(ProtocallVersion);
        System.arraycopy(reqProtocolVersionBytes.array(), 0, packetBuffer, currentIdx, 2);
        currentIdx += 2;

        ByteBuffer packetLengthBytes = ByteBuffer.allocate(2).putShort((short) (packetBuffer.length - 16));
        System.arraycopy(packetLengthBytes.array(), 0, packetBuffer, currentIdx, 2);
        currentIdx += 2;

        Arrays.fill(packetBuffer, currentIdx, currentIdx + 4, (byte) 0);
        currentIdx += 4;

        ByteBuffer serverIdBytes = ByteBuffer.allocate(4).putInt(serverId);
        System.arraycopy(serverIdBytes.array(), 0, packetBuffer, currentIdx, 4);
        currentIdx += 4;

        return currentIdx;
    }

    private void FinishPacket(byte[] packetBufer) {
        Arrays.fill(packetBufer, 8, 12, (byte) 0); // clear the bytes at index 8 to 11

        CRC32 crc32 = new CRC32();
        crc32.update(packetBufer);
        long crcCalc = crc32.getValue();

        ByteBuffer crcBytes = ByteBuffer.allocate(4).putInt((int) crcCalc);
        System.arraycopy(crcBytes.array(), 0, packetBufer, 8, 4);
    }
}