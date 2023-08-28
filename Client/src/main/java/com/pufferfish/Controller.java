package com.pufferfish;

public class Controller {

    class PhysicalAddress {
        byte[] mac;
        byte[] ip;
        byte[] port;

        public PhysicalAddress(byte[] mac) {
            this.mac = mac;
        }

        public PhysicalAddress() {
            this.mac = null;
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

    public class DPad {

        public boolean north;
        public boolean south;
        public boolean west;
        public boolean east;
        public Object Direction;

        public DPad() {
            north = false;
            south = false;
            east = false;
            west = false;
        }
    }

    public class Buttons {
        public boolean a;
        public boolean b;
        public boolean one;
        public boolean two;
        public boolean plus;
        public boolean minus;
        public boolean home;
    }

    String id;
    int PadId = 0;
    int model = 1;
    byte battery = 0x05;
    int connected = 0;
    int connection = 0;
    PhysicalAddress PadMacAddress = new PhysicalAddress(new byte[] { 01, 02, 03, 04, 05, 06 });
    public int packageCounter = 0;
    public DPad dpad = new DPad();
    public Buttons buttons = new Buttons();

    public Controller(String id) {
        this.id = id;
        connected = 2;
    }

    private static boolean[] decodeInt(int encodedInt) {
        int arrayLength = Integer.SIZE;
        if (arrayLength <= 0) {
            throw new IllegalArgumentException("Array length must be greater than 0.");
        }

        int[] intArray = new int[arrayLength];
        for (int i = arrayLength - 1; i >= 0; i--) {
            intArray[i] = encodedInt & 1;
            encodedInt >>= 1;
        }

        boolean[] boolArray = new boolean[arrayLength];

        for (int i = 0; i < intArray.length; i++) {
            boolArray[i] = (intArray[i] == 1 ? true : false);
        }
        return boolArray;
    }

    public void updateState(int encodedInt) {
        boolean[] data = decodeInt(encodedInt);
        for (int i = 0; i < data.length; i++) {
            switch (i) {
                case 0:
                    dpad.north = data[i];
                    break;
                case 1:
                    dpad.south = data[i];
                    break;
                case 2:
                    dpad.west = data[i];
                    break;
                case 3:
                    dpad.east = data[i];
                    break;
                case 4:
                    buttons.b = data[i];
                    break;
                case 5:
                    buttons.one = data[i];
                    break;
                case 6:
                    buttons.two = data[i];
                    break;
                case 7:
                    buttons.plus = data[i];
                    break;
                case 8:
                    buttons.minus = data[i];
                    break;
                case 9:
                    buttons.a = data[i];
                    break;
            }
        }
    }

    public String getSocket() {
        return id;
    }

}
