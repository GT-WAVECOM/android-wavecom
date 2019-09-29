package edu.gatech.xcwang.wavecom.NetUtils;

import java.net.InetAddress;

public class IPEndPoint {
    public static final int MaxPort = 0x0000FFFF;
    public static final int MinPort = 0x00000000;

    private InetAddress ipAddress;
    private int port;

    public IPEndPoint(InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public boolean equals(IPEndPoint endPoint) {
        return endPoint.getIpAddress().equals(ipAddress) && endPoint.getPort() == port;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String toString() {
        return ipAddress.toString() + " : " + port;
    }
}
