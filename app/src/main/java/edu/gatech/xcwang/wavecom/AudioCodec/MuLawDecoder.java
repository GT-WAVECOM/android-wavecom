package edu.gatech.xcwang.wavecom.AudioCodec;

public class MuLawDecoder {
    private static short[] muLawToPcmMap;

    static {
        muLawToPcmMap = new short[256];
        for (byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
            muLawToPcmMap[i + 128] = decode(i);
        }
    }

    private static short decode(byte mulaw) {
        mulaw = (byte)~mulaw;
        int sign = mulaw & 0x80;
        int exponent = (mulaw & 0x70) >> 4;
        int data = mulaw & 0x0f;

        data |= 0x10;

        data <<= 1;
        data += 1;

        data <<= exponent + 2;
        data -= MuLawEncoder.BIAS;
        return (short)(sign == 0 ? data : -data);
    }

    public static short MuLawDecode(byte mulaw) {
        return muLawToPcmMap[mulaw];
    }

    public static short[] MuLawDecode(byte[] data) {
        int size = data.length;
        short[] decoded = new short[size];
        for (int i = 0; i < size; i++)
            decoded[i] = muLawToPcmMap[data[i] + 128];
        return decoded;
    }

    public static void MuLawDecode(byte[] data, short[] decoded) {
        int size = data.length;
        decoded = new short[size];
        for (int i = 0; i < size; i++)
            decoded[i] = muLawToPcmMap[data[i]];
    }

    public static void MuLawDecode(byte[] data, byte[] decoded) {
        int size = data.length;
        decoded = new byte[size * 2];
        for (int i = 0; i < size; i++) {
            //First byte is the less significant byte
            decoded[2 * i] = (byte)(muLawToPcmMap[data[i]] & 0xff);
            //Second byte is the more significant byte
            decoded[2 * i + 1] = (byte)(muLawToPcmMap[data[i]] >> 8);
        }
    }
}
