package be.twofold.tinynes;

public final class Cartridge {

    private final Mapper mapper;
    private final byte[] prg;
    private final byte[] chr;
    private final byte[] ram;

    public Cartridge(Rom rom) {
        int prgBanks = rom.getPrg().length / 0x4000;
        int chrBanks = rom.getChr().length / 0x2000;
        this.mapper = createMapper(rom.getMapperId(), prgBanks, chrBanks);
        this.prg = rom.getPrg();
        this.chr = rom.getChr();
        this.ram = createRam(rom.getMapperId());
    }

    private static Mapper createMapper(int mapperId, int prgBanks, int chrBanks) {
        return switch (mapperId) {
            case 0 -> new Mapper000(prgBanks, chrBanks);
            case 1 -> new Mapper001(prgBanks, chrBanks);
            default -> throw new IllegalArgumentException("Unknown mapper: " + mapperId);
        };
    }

    private byte[] createRam(int mapperId) {
        return switch (mapperId) {
            case 0 -> null;
            case 1 -> new byte[0x2000];
            default -> throw new IllegalArgumentException("Unknown mapper: " + mapperId);
        };
    }

    public byte cpuRead(int address) {
        if (ram != null && address >= 0x6000 && address <= 0x7FFF) {
            return ram[address & 0x1FFF];
        }
        return prg[mapper.cpuRead(address)];
    }

    public void cpuWrite(int address, byte value) {
        if (ram != null && address >= 0x6000 && address <= 0x7FFF) {
            ram[address & 0x1FFF] = value;
        }
        mapper.cpuWrite(address, value);
    }

    public byte ppuRead(int address) {
        return chr[mapper.ppuRead(address)];
    }

    public void ppuWrite(int address, byte value) {
        mapper.ppuWrite(address, value);
    }

}
