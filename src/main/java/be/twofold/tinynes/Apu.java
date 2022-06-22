package be.twofold.tinynes;

public final class Apu {

    public byte read(int address) {
        // System.out.println("Apu.read(" + Util.hex4(address) + ")");
        return 0;
    }

    public void write(int address, int data) {
        // System.out.println("Apu.write(" + Util.hex4(address) + ", " + Util.hex2(data) + ")");
    }

}
