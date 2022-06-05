package be.twofold.tinynes;

public final class Cartridge {

    private final Mapper mapper;
    private final byte[] prg;
    private final byte[] chr;

    public Cartridge(Rom rom) {
        int prgBanks = rom.getPrg().length / 0x4000;
        int chrBanks = rom.getChr().length / 0x2000;
        this.mapper = createMapper(rom.getMapperId(), prgBanks, chrBanks);
        this.prg = rom.getPrg();
        this.chr = rom.getChr();
    }

    private static Mapper createMapper(int mapperId, int prgBanks, int chrBanks) {
        return switch (mapperId) {
            case 0 -> new Mapper000(prgBanks, chrBanks);
            default -> throw new IllegalArgumentException("Unknown mapper: " + mapperId);
        };
    }

    public byte cpuRead(int address) {
        return prg[mapper.cpuRead(address)];
    }

    public void cpuWrite(int address, byte value) {
        mapper.cpuWrite(address, value);
    }

    public byte ppuRead(int address) {
        return chr[mapper.ppuRead(address)];
    }

    public void ppuWrite(int address, byte value) {
        mapper.ppuWrite(address, value);
    }

}
