package be.twofold.tinynes;

public class Mapper001 extends Mapper {
    private int loader;
    private int loaderCount;
    private int control;
    private int select4Lo;
    private int select4Hi;
    private int select8;
    private int select16Lo;
    private int select16Hi;
    private int select32;
    private Mirror mirror;

    public Mapper001(int prgBanks, int chrBanks) {
        super(prgBanks, chrBanks);
    }

    @Override
    int cpuRead(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
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
        }
        throw illegalRead(address);
    }

    @Override
    void cpuWrite(int address, byte value) {
        if (address >= 0x8000) {
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
                            mirror = switch (control & 0x03) {
                                case 0 -> Mirror.ONE_SCREEN_LO;
                                case 1 -> Mirror.ONE_SCREEN_HI;
                                case 2 -> Mirror.VERTICAL;
                                case 3 -> Mirror.HORIZONTAL;
                                default -> throw new IllegalStateException("Unexpected value: " + (control & 0x03));
                            };
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

        }
    }

    @Override
    int ppuRead(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            return address;
        }
        throw illegalRead(address);
    }

    @Override
    void ppuWrite(int address, byte value) {
        throw illegalWrite(address, value);
    }

}
