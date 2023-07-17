package com.pufferfish;

public class Controller {
    String id;
    int PadId;
    int constants;
    int model;
    int battery;
    int connection;
    byte PadMacAdress[];

    public Controller(String id) {
        this.id = id;
    }

    public void pressButton(int ButtonID){

    }

    public String getSocket(){
        return id;
    }
}