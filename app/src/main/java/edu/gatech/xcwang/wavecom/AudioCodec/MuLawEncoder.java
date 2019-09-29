package edu.gatech.xcwang.wavecom.AudioCodec;

public class MuLawEncoder {
    public static final int BIAS = 0x84;
    public static final int MAX = 32635;

    public static boolean zeroTrap;
    private static byte[] pcmToMuLawMap;

    public static boolean isZeroTrap() {
        return pcmToMuLawMap[33000] != 0;
    }

    public static void setZeroTrap() {
        byte val = (byte)(zeroTrap ? 2 : 0);
    }

    static {
        pcmToMuLawMap = new byte[65536];
        for (int i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            pcmToMuLawMap[(i & 0xffff)] = encode(i);
        }
    }

    public MuLawEncoder() {}

    private static byte encode(int pcm) {
        int sign = (pcm & 0x8000) >> 8;
        if (sign != 0) {
            pcm = -pcm;
        }
        if (pcm > MAX) {
            pcm = MAX;
        }
        pcm += BIAS;

        int exponent = 7;
        for (int expMask = 0x4000; (pcm & expMask) == 0; exponent--, expMask >>= 1) {}

        int mantissa = (pcm >> (exponent + 3)) & 0x0f;

        byte mulaw = (byte)(sign | exponent << 4 | mantissa);
        return (byte)(~mulaw);
    }

    public static byte MuLawEncode(int pcm) {
        return pcmToMuLawMap[pcm & 0xffff];
    }

    public static byte MuLawEncode(short pcm) {
        return pcmToMuLawMap[pcm & 0xffff];
    }

    public static byte[] MuLawEncode(int[] data) {
        int size = data.length;
        byte[] encoded = new byte[size];
        for (int i = 0; i < size; i++) {
            encoded[i] = MuLawEncode(data[i]);
        }
        return encoded;
    }

    public static byte[] MuLawEncode(short[] data) {
        int size = data.length;
        byte[] encoded = new byte[size];
        for (int i = 0; i < size; i++) {
            encoded[i] = MuLawEncode(data[i]);
        }
        return encoded;
    }

    public static byte[] MuLawEncode(byte[] data) {
        int size = data.length / 2;
        byte[] encoded = new byte[size];
        for (int i = 0; i < size; i++) {
            encoded[i] = MuLawEncode((data[2 * i + 1] << 8) | data[2 * i]);
        }
        return encoded;
    }

    public static void MuLawEncode(byte[] data, byte[] target) {
        int size = data.length / 2;
        for (int i = 0; i < size; i++) {
            target[i] = MuLawEncode((data[2 * i + 1] << 8) | data[2 * i]);
        }
    }

    //based on Android AudioRecord class, the raw mic data is set to PCM_16_BIT and stored in short[] array
    public static byte[] MuLawEncode(short[] data, int length) {
        byte[] encoded = new byte[length];

        for (int i = 0; i < length; i++) {
            // '/ 4' is for adjust the volume
            encoded[i] = MuLawEncode(data[i] / 4);;
        }
        return encoded;
    }
}
