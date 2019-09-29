package edu.gatech.xcwang.wavecom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.net.DatagramSocket;
import java.net.SocketException;

import edu.gatech.xcwang.wavecom.AudioStreaming.LiveChat;

public class MainActivity extends AppCompatActivity {
    private static DatagramSocket Voip_socket;
    private static final int REQUEST_PERMISSION = 123;
    private static String deviceId = "abcdefgh";
    private Button connectServerButton;
    private Button walkiTalkiBtn;
    private LiveChat chatClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get the permission for the audio record
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_PERMISSION);
        }

        try {
            Voip_socket = new DatagramSocket();
            Voip_socket.setReuseAddress(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        connectServerButton = (Button) findViewById(R.id.connect_to_server_button);
        connectServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateCall();
            }
        });

        walkiTalkiBtn = (Button) findViewById(R.id.walki_talki);
        walkiTalkiBtn.setVisibility(View.INVISIBLE);
    }

    private void initiateCall() {
        Thread callThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    chatClient = new LiveChat(deviceId, Voip_socket, MainActivity.this);
                    boolean initRes = chatClient.chatInit();

                    if (initRes) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                walkiTalkiBtn.setVisibility(View.VISIBLE);
                                walkiTalkiBtn.setOnTouchListener(new View.OnTouchListener() {
                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                        switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN:
                                                chatClient.startTalk();
                                                break;
                                            case MotionEvent.ACTION_UP:
                                                chatClient.stopTalk();
                                                break;
                                        }
                                        return false;
                                    }
                                });
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cancelCall();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        callThread.start();
    }

    private void cancelCall() {
        walkiTalkiBtn.setVisibility(View.INVISIBLE);
        Thread cancelCallThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (chatClient != null) {
                    chatClient.chatTerminate();
                }
            }
        });
        cancelCallThread.start();
    }
}
