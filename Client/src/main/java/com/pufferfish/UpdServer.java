package com.pufferfish;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

public class UpdServer {

    private Socket udpSocket;
    private int serverId;
    private boolean isRunning;
    private byte[] receiveBuffer = new byte[1024];

    enum MessageType {
        DSUC_VersionReq(0x100000),
        DSUS_VersionRsp(0x100000),
        DSUC_ListPorts(0x100001),
        DSUS_PortInfo = (0x100001),
        DSUC_PadDataReq = 0x100002,
        DSUS_PadDataRsp = 0x100002,
    };

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

            if (messageType = (int) MessageType.DSUC_VersionReq){

            }


        } catch (Exception e) {
            // TODO: handle exception
        }
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