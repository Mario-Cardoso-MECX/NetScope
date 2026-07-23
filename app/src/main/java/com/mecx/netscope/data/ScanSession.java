package com.mecx.netscope.data;

public class ScanSession {
    private String subnet;
    private String date;
    private int totalHosts;
    private int threats;

    public ScanSession(String subnet, String date, int totalHosts, int threats) {
        this.subnet = subnet;
        this.date = date;
        this.totalHosts = totalHosts;
        this.threats = threats;
    }

    public String getSubnet() { return subnet; }
    public String getDate() { return date; }
    public int getTotalHosts() { return totalHosts; }
    public int getThreats() { return threats; }

    public void setTotalHosts(int totalHosts) { this.totalHosts = totalHosts; }
    public void setThreats(int threats) { this.threats = threats; }
}