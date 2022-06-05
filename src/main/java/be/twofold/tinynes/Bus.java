package be.twofold.tinynes;

public final class Bus {

    private final Cartridge cartridge;
    private final byte[] ram = new byte[2 * 1024];

    public Bus(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    public byte read(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            return ram[address & 0x07FF];
        }
        if (address >= 0x2000 && address <= 0x3FFF) {
            return 0;
        }
        if (address >= 0x4000 && address <= 0x401F) {
            return 0;
        }
        if (address >= 0x4020 && address <= 0xFFFF) {
            return cartridge.cpuRead(address);
        }
        throw new IllegalArgumentException("Invalid address: 0x" + Integer.toHexString(address));
    }

    public void write(int address, byte value) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            ram[address & 0x07FF] = value;
            return;
        }
        if (address >= 0x2000 && address <= 0x3FFF) {
            return;
        }
        if (address >= 0x4000 && address <= 0x401F) {
            return;
        }
        if (address >= 0x4020 && address <= 0xFFFF) {
            cartridge.cpuWrite(address, value);
            return;
        }
        throw new IllegalArgumentException("Invalid address: 0x" + Integer.toHexString(address));
    }

}
