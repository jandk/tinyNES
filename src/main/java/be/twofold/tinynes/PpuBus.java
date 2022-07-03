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
            return nameTable[nameTableAddress(address)];
        }

        if (address <= 0x3FFF) {
            return palette[paletteAddress(address)];
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
            nameTable[nameTableAddress(address)] = value;
            return;
        }

        if (address <= 0x3FFF) {
            palette[paletteAddress(address)] = value;
            return;
        }

        throw new IllegalArgumentException("Illegal PPU write: $" + Integer.toHexString(address));
    }

    private int nameTableAddress(int address) {
        return switch (cartridge.getMirroringMode()) {
            case VERTICAL -> address & 0x7FF;
            case HORIZONTAL -> (address >>> 1) & 0x0400 | address & 0x03FF;
            default -> address & ~0x2000;
        };
    }

    private int paletteAddress(int address) {
        if ((address & 0x13) == 0x10) {
            address &= ~0x10;
        }
        return address & 0x1F;
    }

}
