package be.twofold.tinynes;

public final class Mapper001 extends Mapper {
    private int loader;
    private int control;
    private int select4Lo;
    private int select4Hi;
    private int select8;
    private int select16Lo;
    private int select16Hi;
    private int select32;

    public Mapper001(int prgBanks, int chrBanks) {
        super(prgBanks, chrBanks);
        reset();
    }

    // region Properties

    private MirroringMode mirroring() {
        return switch (control & 0x03) {
            case 0 -> MirroringMode.HORIZONTAL;
            case 1 -> MirroringMode.VERTICAL;
            case 2 -> MirroringMode.ONE_SCREEN_LO;
            case 3 -> MirroringMode.ONE_SCREEN_HI;
            default -> throw new IllegalStateException("Unexpected value: " + (control & 0x03));
        };
    }

    private boolean bank16k() {
        return (control & 0x08) != 0;
    }

    private int prgBankMode() {
        return (control >> 2) & 0x03;
    }

    private boolean chrBankMode() {
        return (control & 0x10) != 0;
    }

    // endregion

    @Override
    int cpuRead(int address) {
        assert address >= 0x8000 && address <= 0xFFFF;

        if (bank16k()) {
            if (address <= 0xBFFF) {
                return (select16Lo << 14) | (address & 0x3FFF);
            }

            return (select16Hi << 14) | (address & 0x3FFF);
        }
        return (select32 << 15) | (address & 0x7FFF);
    }

    @Override
    void cpuWrite(int address, byte value) {
        assert address >= 0x8000 && address <= 0xFFFF;

        // Reset shift register
        if ((value & 0x80) != 0) {
            loader = 0x10;
            control |= 0x0C;
            return;
        }

        // Because we loaded with 0b10000, we recognize the 1 as being finished
        boolean loaderFinished = (loader & 0x01) != 0;
        loader = (loader >> 1) | (value & 0x01) << 4;
        if (!loaderFinished) {
            return;
        }

        // Now we look at what internal register we want to set
        switch ((address >> 13) & 0x03) {
            case 0:
                control = loader & 0x1F;
                break;
            case 1:
                if (chrBankMode()) {
                    select4Lo = loader & 0x1F;
                } else {
                    select8 = loader & 0x1E;
                }
                break;
            case 2:
                if (chrBankMode()) {
                    select4Hi = loader & 0x1F;
                }
                break;
            case 3:
                switch (prgBankMode()) {
                    case 0, 1 -> select32 = (loader & 0x0E) >> 1;
                    case 2 -> {
                        select16Lo = 0;
                        select16Hi = loader & 0x0F;
                    }
                    case 3 -> {
                        select16Lo = loader & 0x0F;
                        select16Hi = prgBanks - 1;
                    }
                }
                break;
        }

        loader = 0x10;
    }

    @Override
    int ppuRead(int address) {
        assert address >= 0x0000 && address <= 0x1FFF;

        if (chrBanks == 0) {
            return address;
        }
        if ((control & 0b10000) != 0) {
            if (address <= 0x0FFF) {
                return select4Lo * 0x1000 + (address & 0x0FFF);
            }
            return select4Hi * 0x1000 + (address & 0x0FFF);
        }
        return select8 * 0x2000 + (address & 0x1FFF);
    }

    @Override
    void ppuWrite(int address, byte value) {
        assert address >= 0x0000 && address <= 0x1FFF;

        // throw illegalWrite(address, value);
    }

    @Override
    void reset() {
        control = 0x1C;
        loader = 0x10;

        select4Lo = 0;
        select4Hi = 0;
        select8 = 0;
        select16Lo = 0;
        select16Hi = prgBanks - 1;
        select32 = 0;
    }

}
