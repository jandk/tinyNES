package be.twofold.tinynes;

public final class Mapper001 extends Mapper {
    private int loader;
    private int loaderCount;
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

    private Mirror mirroring() {
        return switch (control & 0x03) {
            case 0 -> Mirror.HORIZONTAL;
            case 1 -> Mirror.VERTICAL;
            case 2 -> Mirror.ONE_SCREEN_LO;
            case 3 -> Mirror.ONE_SCREEN_HI;
            default -> throw new IllegalStateException("Unexpected value: " + (control & 0x03));
        };
    }

    private int prgBankMode() {
        return (control >> 2) & 0x02;
    }

    private int chrBankMode() {
        return (control >> 4) & 0x01;
    }

    // endregion

    @Override
    int cpuRead(int address) {
        if ((control & 0x08) != 0) {
            if (address >= 0x8000 && address <= 0xBFFF) {
                return select16Lo * 0x4000 + (address & 0x3FFF);
            }

            if (address >= 0xC000 && address <= 0xFFFF) {
                return select16Hi * 0x4000 + (address & 0x3FFF);
            }
        } else {
            return select32 * 0x8000 + (address & 0x7FFF);
        }
        throw illegalRead(address);
    }

    @Override
    void cpuWrite(int address, byte value) {
        if (address < 0x8000) {
            throw illegalWrite(address, value);
        }
        if ((value & 0x80) != 0) {
            loader = 0x00;
            loaderCount = 0;
            control = control | 0x0C;
        } else {
            loader = (loader >> 1) | (value & 0x01) << 4;
            loaderCount++;

            if (loaderCount == 5) {
                int target = (address >> 13) & 0x03;

                switch (target) {
                    case 0:
                        control = loader & 0x1F;
                        break;
                    case 1:
                        if ((control & 0x10) != 0) {
                            select4Lo = loader & 0x1F;
                        } else {
                            select8 = loader & 0x1E;
                        }
                        break;
                    case 2:
                        if ((control & 0x10) != 0) {
                            select4Hi = loader & 0x1F;
                        }
                        break;
                    case 3:
                        int nPRGMode = (control >> 2) & 0x03;

                        switch (nPRGMode) {
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

                loader = 0x00;
                loaderCount = 0;
            }

        }

        // throw illegalWrite(address, value);
    }

    @Override
    int ppuRead(int address) {
        if (address < 0x2000) {
            if (chrBanks == 0) {
                return address;
            } else {
                if ((control & 0b10000) != 0) {
                    // 4K CHR Bank Mode
                    if (address >= 0x0000 && address <= 0x0FFF) {
                        return (select4Lo << 12) + (address & 0x0FFF);
                    }

                    if (address >= 0x1000 && address <= 0x1FFF) {
                        return (select4Hi << 12) + (address & 0x0FFF);
                    }
                } else {
                    // 8K CHR Bank Mode
                    return select8 * 0x2000 + (address & 0x1FFF);
                }
            }
        }

        throw illegalRead(address);
    }

    @Override
    void ppuWrite(int address, byte value) {
        if (address >= 0x2000) {
            throw illegalWrite(address, value);
        }
//        if (chrBanks == 0) {
//            return address;
//        }
        throw illegalWrite(address, value);
    }

    @Override
    void reset() {
        control = 0x1C;
        loader = 0;
        loaderCount = 0;

        select4Lo = 0;
        select4Hi = 0;
        select8 = 0;
        select16Lo = 0;
        select16Hi = prgBanks - 1;
        select32 = 0;
    }

}
