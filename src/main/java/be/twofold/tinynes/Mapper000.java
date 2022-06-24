package be.twofold.tinynes;

final class Mapper000 extends Mapper {

    Mapper000(int prgBanks, int prgSize) {
        super(prgBanks, prgSize);
    }

    @Override
    int cpuRead(int address) {
        assert address >= 0x8000 && address <= 0xFFFF;

        return address & (prgBanks > 1 ? 0x7FFF : 0x3FFF);
    }

    @Override
    void cpuWrite(int address, byte value) {
        assert address >= 0x8000 && address <= 0xFFFF;

        throw illegalWrite(address, value);
    }

    @Override
    int ppuRead(int address) {
        assert address >= 0x0000 && address <= 0x1FFF;

        return address;
    }

    @Override
    void ppuWrite(int address, byte value) {
        assert address >= 0x0000 && address <= 0x1FFF;

        throw illegalWrite(address, value);
    }

    @Override
    void reset() {
    }

}
