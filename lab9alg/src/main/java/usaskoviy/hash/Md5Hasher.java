package usaskoviy.hash;

import java.nio.charset.StandardCharsets;

/**
 * Собственная реализация MD5 для лабораторной работы.
 */
public final class Md5Hasher {

    private static final int[] SHIFT = {
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    };

    private static final int[] TABLE = new int[64];

    static {
        for (int i = 0; i < 64; i++) {
            TABLE[i] = (int) (long) Math.floor(Math.abs(Math.sin(i + 1)) * (1L << 32));
        }
    }

    private Md5Hasher() {
    }

    public static String hashHex(String input) {
        return hashHex(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashHex(byte[] input) {
        int a0 = 0x67452301;
        int b0 = 0xEFCDAB89;
        int c0 = 0x98BADCFE;
        int d0 = 0x10325476;

        byte[] padded = pad(input);
        int[] words = new int[16];

        for (int offset = 0; offset < padded.length; offset += 64) {
            for (int i = 0; i < 16; i++) {
                int base = offset + i * 4;
                words[i] = (padded[base] & 0xFF)
                        | ((padded[base + 1] & 0xFF) << 8)
                        | ((padded[base + 2] & 0xFF) << 16)
                        | ((padded[base + 3] & 0xFF) << 24);
            }

            int a = a0;
            int b = b0;
            int c = c0;
            int d = d0;

            for (int i = 0; i < 64; i++) {
                int f;
                int g;
                if (i < 16) {
                    f = (b & c) | ((~b) & d);
                    g = i;
                } else if (i < 32) {
                    f = (d & b) | ((~d) & c);
                    g = (5 * i + 1) % 16;
                } else if (i < 48) {
                    f = b ^ c ^ d;
                    g = (3 * i + 5) % 16;
                } else {
                    f = c ^ (b | (~d));
                    g = (7 * i) % 16;
                }

                int temp = d;
                d = c;
                c = b;
                int sum = a + f + TABLE[i] + words[g];
                b = b + Integer.rotateLeft(sum, SHIFT[i]);
                a = temp;
            }

            a0 += a;
            b0 += b;
            c0 += c;
            d0 += d;
        }

        return toHexLittleEndian(a0)
                + toHexLittleEndian(b0)
                + toHexLittleEndian(c0)
                + toHexLittleEndian(d0);
    }

    private static byte[] pad(byte[] input) {
        long bitLen = (long) input.length * 8L;
        int newLength = input.length + 1;
        while (newLength % 64 != 56) {
            newLength++;
        }
        byte[] output = new byte[newLength + 8];
        System.arraycopy(input, 0, output, 0, input.length);
        output[input.length] = (byte) 0x80;

        for (int i = 0; i < 8; i++) {
            output[newLength + i] = (byte) ((bitLen >>> (8 * i)) & 0xFF);
        }
        return output;
    }

    private static String toHexLittleEndian(int value) {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 4; i++) {
            int b = (value >>> (8 * i)) & 0xFF;
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
