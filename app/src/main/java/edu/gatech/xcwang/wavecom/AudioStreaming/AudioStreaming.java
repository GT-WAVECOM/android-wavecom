package edu.gatech.xcwang.wavecom.AudioStreaming;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import edu.gatech.xcwang.wavecom.AudioCodec.MuLawDecoder;
import edu.gatech.xcwang.wavecom.AudioCodec.MuLawEncoder;
import edu.gatech.xcwang.wavecom.NetUtils.IPEndPoint;

public class AudioStreaming {

    private static final String TAG = "AudioStreaming";
    private DatagramSocket socket;
    private IPEndPoint master;
    private boolean isReceiving = true;
    private boolean isSending = true;

    //parameter for audio
    private static final int SAMPLE_RATE = 16000;
    private static final int INPUT_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int OUTPUT_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_UNIT_LENGTH = 512;
    private static final int INPUT_BUFFER_SIZE =
            (AudioRecord.getMinBufferSize(SAMPLE_RATE, INPUT_CHANNEL, FORMAT) / BUFFER_UNIT_LENGTH + 1) * BUFFER_UNIT_LENGTH;
    private static final int OUTPUT_BUFFER_SIZE =
            (AudioTrack.getMinBufferSize(SAMPLE_RATE, OUTPUT_CHANNEL, FORMAT) / BUFFER_UNIT_LENGTH) * BUFFER_UNIT_LENGTH;

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;

    private short[][] inputBuffers;
    private int inputBufferNums;
    private int inputBuffersIndex;

    private byte[][] outputBuffers;
    private int outputBufferNums;
    private int outPutBuffersIndex;

    public AudioStreaming() {
        //AudioRecord test
//        int[] mSampleRates = new int[] { 44100, 22050, 11025, 16000, 8000 };
//        for (int rate : mSampleRates) {
//            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
//                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
//                    try {
//                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
//                                + channelConfig);
//                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
//
//                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
//                            // check if we can instantiate and have a success
//                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channelConfig, audioFormat, bufferSize);
//
//                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
//                                Log.d(TAG, "Success at rate: " + rate + " format: " + audioFormat + " channel: " + channelConfig);
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, rate + "Exception, keep trying.",e);
//                    }
//                }
//            }
//        }

        //AudioTrack test
//        int[] mSampleRates = new int[] { 44100, 22050, 11025, 16000, 8000 };
//        for (int rate : mSampleRates) {
//            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
//                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
//                    try {
//                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
//                                + channelConfig);
//                        int bufferSize = AudioTrack.getMinBufferSize(rate, channelConfig, audioFormat);
//
//                        if (bufferSize != AudioTrack.ERROR_BAD_VALUE) {
//                            // check if we can instantiate and have a success
//                            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, rate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);
//
//                            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
//                                Log.d(TAG, "Success at rate: " + rate + " format: " + audioFormat + " channel: " + channelConfig + " buffer size: " + bufferSize);
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, rate + "Exception, keep trying.",e);
//                    }
//                }
//            }
//        }
    }

    public AudioStreaming(DatagramSocket socket, IPEndPoint master) {
        this.socket = socket;
        this.master = master;
    }

    public void startAudioStream() {
        //mic --> UDP
        inputBufferNums = 10;
        inputBuffers = new short[inputBufferNums][INPUT_BUFFER_SIZE];
        inputBuffersIndex = 0;
        prepareInputAudio();
        //UDP --> speaker
        outputBufferNums = 10;
        outputBuffers = new byte[outputBufferNums][BUFFER_UNIT_LENGTH];
        outPutBuffersIndex = 0;
        prepareOutputAudio();

        System.out.println("Input buffer size: " + INPUT_BUFFER_SIZE);
        System.out.println("Output buffer size: " + OUTPUT_BUFFER_SIZE);
    }

    public void stopAudioStream() {
        isReceiving = false;
        isSending = false;
    }

    //walki-talki communication function
    public void talk() {
        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
        }
    }

    public void stopTalk() {
        if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.stop();
        }
    }

    //mic --> UDP
    private void prepareInputAudio() {

        // heart beat thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramPacket sendingPacket;
                    byte[] hearBeat = new byte[] {'2'};

                    while (isSending) {
                        // while not talking, send heart beat
                        sendingPacket = new DatagramPacket(hearBeat, 1, master.getIpAddress(), master.getPort());
                        socket.send(sendingPacket);
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    Log.d(TAG, e + "");
                }
            }
        }).start();

        // voice send out thread
        Thread inputAudioStream = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, INPUT_CHANNEL, FORMAT, INPUT_BUFFER_SIZE);
                    //acoustic echo canceller
                    int audioSessionId = audioRecord.getAudioSessionId();
                    if (AcousticEchoCanceler.isAvailable()) {
                        AcousticEchoCanceler.create(audioSessionId);
                    }
                    if (NoiseSuppressor.isAvailable()) {
                        NoiseSuppressor.create(audioSessionId);
                    }
                    //start to send out sound data packets
                    DatagramPacket sendingPacket;

                    while (isSending) {
                        int read = audioRecord.read(inputBuffers[inputBuffersIndex], 0, inputBuffers[inputBuffersIndex].length);
                        //read ==> number of frames read in
                        byte[] toSend;
                        if (read != 0) {
                            //break the buffer into small packets with 512 bytes length, ESP_32 only receiving this size for now
                            int numOfPackets = inputBuffers[inputBuffersIndex].length / BUFFER_UNIT_LENGTH;
                            for (int i = 0; i < numOfPackets; i++) {
                                toSend = MuLawEncoder.MuLawEncode(Arrays.copyOfRange(inputBuffers[inputBuffersIndex],
                                        i * BUFFER_UNIT_LENGTH, (i + 1) * BUFFER_UNIT_LENGTH), BUFFER_UNIT_LENGTH);
                                sendingPacket = new DatagramPacket(toSend, toSend.length, master.getIpAddress(), master.getPort());
                                socket.send(sendingPacket);
//                                Thread.sleep(8);
                            }

                            inputBuffersIndex = (inputBuffersIndex + 1) % inputBufferNums;
                        }
                    }
                    audioRecord.stop();
                    audioRecord.release();
                } catch (Exception e) {
                    e.printStackTrace();
                    if (audioRecord != null) {
                        audioRecord.stop();
                        audioRecord.release();
                    }
                }
            }
        });
        inputAudioStream.start();
    }

    //UDP --> speaker
    private void prepareOutputAudio() {
        Thread outputAudioStream = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    audioTrack = new AudioTrack(AudioAttributes.USAGE_VOICE_COMMUNICATION, SAMPLE_RATE, OUTPUT_CHANNEL, FORMAT, OUTPUT_BUFFER_SIZE, AudioTrack.MODE_STREAM);
                    DatagramPacket receivingPacket;
                    short[] toWrite;
                    audioTrack.play();
                    socket.setSoTimeout(5000);
                    while (isReceiving) {
                        receivingPacket = new DatagramPacket(outputBuffers[outPutBuffersIndex], BUFFER_UNIT_LENGTH);
                        socket.receive(receivingPacket);
//                        Log.d(TAG, receivingPacket.getLength() + "");
                        if (receivingPacket.getLength() < BUFFER_UNIT_LENGTH) continue; // skip the first few packets during device ringing
                        toWrite = MuLawDecoder.MuLawDecode(outputBuffers[outPutBuffersIndex]);
                        audioTrack.write(toWrite, 0, toWrite.length);
                        outPutBuffersIndex = (outPutBuffersIndex + 1) % outputBufferNums;
                    }
                    audioTrack.stop();
                    audioTrack.release();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
        outputAudioStream.start();
    }
}
