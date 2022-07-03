package be.twofold.tinynes;

public abstract class Mapper {

    final int prgBanks;
    final int chrBanks;

    Mapper(int prgBanks, int chrBanks) {
        this.prgBanks = prgBanks;
        this.chrBanks = chrBanks;
    }

    abstract int cpuRead(int address);

    abstract void cpuWrite(int address, byte value);

    abstract int ppuRead(int address);

    abstract void ppuWrite(int address, byte value);

    abstract void reset();

    IllegalArgumentException illegalWrite(int address, byte value) {
        return new IllegalArgumentException("Invalid write: " + Util.hex4(address));
    }

}
