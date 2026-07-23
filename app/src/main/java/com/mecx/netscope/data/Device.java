package com.mecx.netscope.data;

public class Device {
    private String name;
    private String ip;
    private String vendor; // El nuevo campo para la marca

    public Device(String name, String ip) {
        this.name = name;
        this.ip = ip;
        this.vendor = "Genérico"; // Valor por defecto tipo Fing
    }

    public String getName() { return name; }
    public String getIp() { return ip; }
    public String getVendor() { return vendor; }

    public void setName(String name) { this.name = name; }
    public void setVendor(String vendor) { this.vendor = vendor; }
}