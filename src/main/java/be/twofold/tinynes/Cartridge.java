package be.twofold.tinynes;

public final class Cartridge {

    private final Mapper mapper;
    private final byte[] prg;
    private final byte[] chr;
    private final byte[] prgRam;
    private final byte[] chrRam;
    private final MirroringMode mirroringMode;

    public Cartridge(Rom rom) {
        int prgBanks = rom.getPrg().length / 0x4000;
        int chrBanks = rom.getChr().length / 0x2000;
        this.mapper = createMapper(rom.getMapperId(), prgBanks, chrBanks);
        this.prg = rom.getPrg();
        this.chr = rom.getChr();
        this.prgRam = createPrgRam(rom.getMapperId());
        this.chrRam = chrBanks == 0 ? new byte[0x2000] : null;
        this.mirroringMode = rom.getMirroringMode();
    }

    private static Mapper createMapper(int mapperId, int prgBanks, int chrBanks) {
        return switch (mapperId) {
            case 0 -> new Mapper000(prgBanks, chrBanks);
            case 1 -> new Mapper001(prgBanks, chrBanks);
            default -> throw new IllegalArgumentException("Unknown mapper: " + mapperId);
        };
    }

    public MirroringMode getMirroringMode() {
        return mirroringMode;
    }

    private byte[] createPrgRam(int mapperId) {
        return switch (mapperId) {
            case 0 -> null;
            case 1 -> new byte[0x2000];
            default -> throw new IllegalArgumentException("Unknown mapper: " + mapperId);
        };
    }

    public byte cpuRead(int address) {
        if (prgRam != null && address >= 0x6000 && address <= 0x7FFF) {
            return prgRam[address & 0x1FFF];
        }
        return prg[mapper.cpuRead(address)];
    }

    public void cpuWrite(int address, byte value) {
        if (prgRam != null && address >= 0x6000 && address <= 0x7FFF) {
            prgRam[address & 0x1FFF] = value;
            return;
        }
        mapper.cpuWrite(address, value);
    }

    public byte ppuRead(int address) {
        assert address >= 0x0000 && address <= 0x1FFF;

        if (chrRam != null) {
            return chrRam[address & 0x1FFF];
        }
        return chr[mapper.ppuRead(address)];
    }

    public void ppuWrite(int address, byte value) {
        assert address >= 0x0000 && address <= 0x1FFF;

        if (chrRam != null) {
            chrRam[address & 0x1FFF] = value;
            return;
        }
        mapper.ppuWrite(address, value);
    }

}
