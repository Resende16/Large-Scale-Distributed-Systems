package edu.sdle;

import java.util.UUID;

public class UUIDUtils {
    // Convert a UUID to a byte array
    public static byte[] uuidToBytes(UUID uuid) {
        byte[] b = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        // msb
        b[0] = (byte) (msb >>> 56);
        b[1] = (byte) (msb >>> 48);
        b[2] = (byte) (msb >>> 40);
        b[3] = (byte) (msb >>> 32);
        b[4] = (byte) (msb >>> 24);
        b[5] = (byte) (msb >>> 16);
        b[6] = (byte) (msb >>> 8);
        b[7] = (byte) (msb);

        // lsb
        b[8]  = (byte) (lsb >>> 56);
        b[9]  = (byte) (lsb >>> 48);
        b[10] = (byte) (lsb >>> 40);
        b[11] = (byte) (lsb >>> 32);
        b[12] = (byte) (lsb >>> 24);
        b[13] = (byte) (lsb >>> 16);
        b[14] = (byte) (lsb >>> 8);
        b[15] = (byte) (lsb);

        return b;
    }

    // Convert a byte array to a UUID
    public static UUID uuidFromBytes(byte[] b) {
        long msb = ((long) (b[0] & 255) << 56) |
                ((long) (b[1] & 255) << 48) |
                ((long) (b[2] & 255) << 40) |
                ((long) (b[3] & 255) << 32) |
                ((long) (b[4] & 255) << 24) |
                ((long) (b[5] & 255) << 16) |
                ((long) (b[6] & 255) << 8)  |
                ((long) (b[7] & 255));

        long lsb = ((long) (b[8] & 255) << 56) |
                ((long) (b[9] & 255) << 48) |
                ((long) (b[10] & 255) << 40) |
                ((long) (b[11] & 255) << 32) |
                ((long) (b[12] & 255) << 24) |
                ((long) (b[13] & 255) << 16) |
                ((long) (b[14] & 255) << 8)  |
                ((long) (b[15] & 255));

        return new UUID(msb, lsb);
    }
}