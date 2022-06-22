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

    IllegalArgumentException illegalRead(int address) {
        return new IllegalArgumentException("Invalid read from address " + Util.hex4(address));
    }

    IllegalArgumentException illegalWrite(int address, byte value) {
        return new IllegalArgumentException(
            String.format("Invalid write to address $%04X with value $%02X", address, value));
    }

    enum Mirror {
        HORIZONTAL,
        VERTICAL,
        ONE_SCREEN_LO,
        ONE_SCREEN_HI
    }

}
