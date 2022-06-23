package be.twofold.tinynes;

public final class PpuBus implements Bus {

    final byte[] nameTable = new byte[0x800];
    final byte[] palette = new byte[0x20];
    final Cartridge cartridge;

    public PpuBus(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    @Override
    public byte read(int address) {
        assert address >= 0x0000;

        if (address <= 0x1FFF) {
            return cartridge.ppuRead(address);
        }

        if (address <= 0x3EFF) {
            return nameTable[address & 0x07FF];
        }

        if (address <= 0x3FFF) {
            return palette[address & 0x001F];
        }

        throw new IllegalArgumentException("Illegal PPU read: $" + Integer.toHexString(address));
    }

    @Override
    public void write(int address, byte value) {
        assert address >= 0x0000;

        if (address <= 0x1FFF) {
            cartridge.ppuWrite(address, value);
            return;
        }

        if (address <= 0x3EFF) {
            nameTable[address & 0x07FF] = value;
            return;
        }

        if (address <= 0x3FFF) {
            int masked = address & 0x1F;
            int newAddress = switch (masked) {
                case 0x0010 -> 0x0000;
                case 0x0014 -> 0x0004;
                case 0x0018 -> 0x0008;
                case 0x001C -> 0x000C;
                default -> masked;
            };
            palette[newAddress] = value;
            return;
        }

        throw new IllegalArgumentException("Illegal PPU write: $" + Integer.toHexString(address));
    }

}
