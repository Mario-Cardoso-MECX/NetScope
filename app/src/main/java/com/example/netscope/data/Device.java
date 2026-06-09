package com.example.netscope.data;

public class Device {
    private String name;
    private String ip;
    private String vendor; // Nuevo campo

    public Device(String name, String ip) {
        this.name = name;
        this.ip = ip;
        this.vendor = "Desconocido"; // Valor por defecto
    }

    public String getName() { return name; }
    public String getIp() { return ip; }
    public String getVendor() { return vendor; }

    public void setName(String name) { this.name = name; }
    public void setVendor(String vendor) { this.vendor = vendor; }
}