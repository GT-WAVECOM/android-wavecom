package edu.gatech.xcwang.wavecom.AudioStreaming;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import java.net.DatagramSocket;
import java.util.Random;

import edu.gatech.xcwang.wavecom.NetUtils.IPEndPoint;

public class LiveChat {
    private static final String TAG = "LiveChat";
    private IPEndPoint target;
    private DatagramSocket Voip_socket;
    private AudioStreaming audioStreaming;
    private Context ringtoneContext;
    private String TURN_Server;
    private int[] TURN_Ports = {7000, 8000, 9000, 10000, 11000, 12000, 13000};
    private int TURN_Port;
    private AppClient appClient;
    private String deviceId;
    private Uri ringtone;
    private boolean isRinging;

    public LiveChat(String deviceId, DatagramSocket Voip_socket, Context ringtoneContext) {
        this.deviceId = deviceId;
        this.Voip_socket = Voip_socket;
        this.ringtoneContext = ringtoneContext;
    }

    public boolean chatInit() {
        TURN_Server = "54.83.79.129";
        Random rand = new Random();
        TURN_Port = TURN_Ports[rand.nextInt(7)];
//        TURN_Port = 11000;

        try {
            appClient = new AppClient(TURN_Server, TURN_Port, Voip_socket);

            //Signaling process through azure-iot-hub message
            isRinging = true;
            //start the ringtone
            ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            Ringtone r = RingtoneManager.getRingtone(ringtoneContext, ringtone);
            r.play();

            CommuMessage peerRes = appClient.RequestForConnection(deviceId);

            //end the ringtone
            r.stop();
            isRinging = false;

            if (peerRes == null) {
                return false;
            }

            target = peerRes.getEndPoint();
            audioStreaming = new AudioStreaming(Voip_socket, target);
            audioStreaming.startAudioStream();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void chatTerminate() {
        if (audioStreaming != null) {
            audioStreaming.stopAudioStream();
        }
        if (appClient != null) {
            appClient.RequestConnectionCancel(isRinging);
        }
        isRinging = false;
    }

    //walki-talki implementation
    public void startTalk() {
        if (audioStreaming != null) {
            audioStreaming.talk();
        }
    }

    public void stopTalk() {
        if(audioStreaming != null) {
            audioStreaming.stopTalk();
        }
    }
}
