package edu.gatech.xcwang.wavecom.AudioStreaming;

import edu.gatech.xcwang.wavecom.NetUtils.IPEndPoint;

public class CommuMessage {
    private IPEndPoint endPoint;
    public CommuMessage(IPEndPoint endPoint) {
        this.endPoint = endPoint;
    }

    public IPEndPoint getEndPoint() {
        return endPoint;
    }

}
