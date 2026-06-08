package com.example.netscope.data;

public class Device {
    private String name;
    private String ip;

    public Device(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    public String getName() { return name; }
    public String getIp() { return ip; }

    public void setName(String name) { this.name = name; }

}