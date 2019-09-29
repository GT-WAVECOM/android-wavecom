package edu.gatech.xcwang.wavecom.AudioStreaming;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import edu.gatech.xcwang.wavecom.NetUtils.IPEndPoint;

public class AppClient {
    private final static String TAG = "AppClient";
    private String masterIp = "127.0.0.1";
    private int masterPort = 7000;
    private IPEndPoint master = null;
    private String pool = "";
    private DatagramSocket socket = null;
    private IPEndPoint target = null;

    public AppClient(String masterIp, int masterPort, DatagramSocket socket)
    {
        this.socket = socket;
        this.masterIp = masterIp;
        this.masterPort = masterPort;
        try {
            master = new IPEndPoint(InetAddress.getByName(masterIp), masterPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private CommuMessage Byte2Address(byte[] data) throws UnknownHostException
    {
        if (data.length != 8)
        {
            Log.d(TAG, "Invalid bytes!");
            return null;
        }
        int offset = 0;
        //Address
        byte[] ip = new byte[4];
        ip[0] = data[offset++];
        ip[1] = data[offset++];
        ip[2] = data[offset++];
        ip[3] = data[offset++];

        //Port
        int firstDigit = byte2int(data[offset++]);
        int secondDigit = byte2int(data[offset++]);
        int port = (firstDigit | secondDigit << 8);

        IPEndPoint endPoint = new IPEndPoint(InetAddress.getByAddress(ip), port);
        return new CommuMessage(endPoint);
    }

    private int byte2int(byte input) {
        int res = 0;
        for (int i = 0; i < 8; i++) {
            int mask = 1 << i;
            res |= input & mask;
        }
        return res;
    }

    public CommuMessage RequestForConnection(String pool)
    {
        this.pool = pool;
        DatagramPacket sendPacket;
        DatagramPacket receivePacket;
        try
        {
            // Request to pool in AWS server
            byte[] requestBytes = (pool + " " + "2").getBytes();
            sendPacket = new DatagramPacket(requestBytes, requestBytes.length, master.getIpAddress(), master.getPort());
            socket.send(sendPacket);

            Log.d(TAG," Request sent, waiting for partner in pool " + pool);

            // Request peer IP address
            byte[] receiveBuffer = new byte[8];
            receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.setSoTimeout(15000); //15s timeout for waiting the response from AWS server
            socket.receive(receivePacket);

            if (new String(receiveBuffer).equals("offline")) {
                Log.d(TAG, "device offline");
                return null;
            } else if (new String(receiveBuffer).equals("cancel!!")) {
                Log.d(TAG, "Request cancelled");
                return null;
            }

            receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.setSoTimeout(15000); //15s timeout for waiting the response from ESP_32
            socket.receive(receivePacket);

            // Parse message
            CommuMessage commuMessage = Byte2Address(receiveBuffer);
            this.target = commuMessage.getEndPoint();
            System.out.println(TAG + String.format(" Connected to %s", target));
            return commuMessage;
        } catch (SocketTimeoutException t) {
            Log.d(TAG, "Socket Timeout in Peer Address Request!");
            t.printStackTrace();
            return null;
        } catch (Exception e) {
            Log.d(TAG, "Socket Error in Peer Address Request!");
            e.printStackTrace();
            return null;
        }
    }

    public void RequestConnectionCancel(boolean isRinging) {
        DatagramPacket sendPacket;
        DatagramPacket receivePacket;
        try {
            byte[] requestBytes;
            byte[]receiveBuffer;
            // if !isRinging, means voice call initiated, need to hang up
            if (!isRinging) {
                requestBytes = ("LC Stop\0").getBytes();
                sendPacket = new DatagramPacket(requestBytes, requestBytes.length, target.getIpAddress(), target.getPort());
                socket.send(sendPacket);
                try {
                    receiveBuffer = new byte[512];
                    socket.setSoTimeout(1000);
                    receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);
                    while (true) {
                        receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(receivePacket);
                        //Log.d(TAG, new String(receiveBuffer));
                    }
                } catch (SocketTimeoutException t) {
                    Log.d(TAG, "Socket Timeout");
                }
                // if isRinging, means during the signaling process, need to cancel;
            } else {
                requestBytes = ("del " + pool).getBytes();
                sendPacket = new DatagramPacket(requestBytes, requestBytes.length, master.getIpAddress(), master.getPort());
                socket.send(sendPacket);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IPEndPoint getMaster() {
        return master;
    }

    public IPEndPoint getTarget() {
        return target;
    }
}
