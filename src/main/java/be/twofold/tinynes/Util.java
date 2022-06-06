package be.twofold.tinynes;

public final class Util {

    private static final byte[] Hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private Util() {
        throw new UnsupportedOperationException();
    }

    public static String hex2(int value) {
        byte[] hex = {'$', '0', '0'};
        hex[1] = Hex[(value & 0xF0) >>> 4];
        hex[2] = Hex[(value & 0x0F) >>> 0];
        return new String(hex);
    }

    public static String hex4(int value) {
        byte[] hex = {'$', '0', '0', '0', '0'};
        hex[1] = Hex[(value & 0xF000) >>> 12];
        hex[2] = Hex[(value & 0x0F00) >>> +8];
        hex[3] = Hex[(value & 0x00F0) >>> +4];
        hex[4] = Hex[(value & 0x000F) >>> +0];
        return new String(hex);
    }

}
