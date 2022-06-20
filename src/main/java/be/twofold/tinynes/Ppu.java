package be.twofold.tinynes;

public final class Ppu {

    private final Cartridge cartridge;

    // Palette
    private final byte[] frameBuffer = new byte[256 * 240];
    private final byte[] nameTable = new byte[0x1000];
    private final byte[] palette = new byte[0x20];
    private final byte[] oam = new byte[0x100];

    // PPU Address Logic
    private boolean latch;
    private int ppuAddrTemp;
    private int ppuAddr;
    private int ppuScrollX;
    private int ppuScrollY;

    private int ppuCtrl;
    private int ppuMask;
    private int ppuStatus;
    private int oamAddr;
    private int oamData;
    private int ppuScroll;
    private int ppuData;

    // Running counters
    boolean nmi;
    int row = 0;
    int col = 0;

    public Ppu(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    // region Flags

    // PPUCTRL


    // PPUMASK

    public boolean grayScale() {
        return (ppuMask & 0x01) != 0;
    }

    public boolean renderBackgroundLeft() {
        return (ppuMask & 0x02) != 0;
    }

    public boolean renderSpritesLeft() {
        return (ppuMask & 0x04) != 0;
    }

    public boolean renderBackground() {
        return (ppuMask & 0x08) != 0;
    }

    public boolean renderSprites() {
        return (ppuMask & 0x10) != 0;
    }

    public boolean emphasizeRed() {
        return (ppuMask & 0x20) != 0;
    }

    public boolean emphasizeGreen() {
        return (ppuMask & 0x40) != 0;
    }

    public boolean emphasizeBlue() {
        return (ppuMask & 0x80) != 0;
    }

    // PPUSTATUS

    public boolean verticalBlank() {
        return (ppuStatus & 0x80) != 0;
    }

    public void setVerticalBlank(boolean value) {
        ppuStatus = (byte) ((ppuStatus & 0x7F) | (value ? 0x80 : 0x00));
    }

    public boolean spriteZeroHit() {
        return (ppuStatus & 0x40) != 0;
    }

    public void setSpriteZeroHit(boolean value) {
        ppuStatus = (byte) ((ppuStatus & 0xBF) | (value ? 0x40 : 0x00));
    }

    public boolean spriteOverflow() {
        return (ppuStatus & 0x20) != 0;
    }

    public void setSpriteOverflow(boolean value) {
        ppuStatus = (byte) ((ppuStatus & 0xDF) | (value ? 0x20 : 0x00));
    }

    // endregion

    // region Properties

    public int getNameTable() {
        return 0x2000 + ((ppuCtrl & 0x03) << 10);
    }

    public int getPatternTable() {
        return (ppuCtrl & 0x10) << 8;
    }

    public boolean isNmiEnabled() {
        return (ppuCtrl & 0x80) != 0;
    }

    // endregion

    public void clock() {
        if (row == 261 && col == 1) {
            setVerticalBlank(false); // Clear VBlank flag
        }
        if (row == 241 && col == 1) {
            setVerticalBlank(true); // Set VBlank flag
            if (isNmiEnabled()) {
                nmi = true;
            }
        }

        col++;
        if (col > 340) {
            col = 0;
            row++;
            if (row > 261) {
                row = 0;
            }
        }
    }

    private byte ppuRead(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            return cartridge.ppuRead(address);
        }
        if (address >= 0x2000 && address <= 0x3FFF) {
            return nameTable[address & 0x0FFF];
        }
        throw new IllegalArgumentException("Illegal PPU read: " + Util.hex4(address));
    }

    private void ppuWrite(int address, byte data) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            cartridge.ppuWrite(address, data);
            return;
        }
        if (address >= 0x2000 && address <= 0x3EFF) {
            address &= 0x0FFF;
            nameTable[address] = data;
            return;
        }
        if (address >= 0X3F00 && address <= 0X3FFF) {
            address = address & 0x1F;
            address = switch (address) {
                case 0x0010 -> 0x0000;
                case 0x0014 -> 0x0004;
                case 0x0018 -> 0x0008;
                case 0x001C -> 0x000C;
                default -> address;
            };
            palette[address] = data;
            return;
        }
        throw new IllegalArgumentException("Illegal PPU write: " + Util.hex4(address));
    }

    public byte cpuRead(int address) {
        return (byte) switch (address & 0x07) {
            case 2 -> readPpuStatus();
            case 4 -> readOam();
            case 7 -> throw new UnsupportedOperationException();
            default -> throw new IllegalArgumentException("Invalid address: " + Util.hex4(address));
        };
    }

    private int readPpuStatus() {
        latch = false;
        return ppuStatus;
    }

    private byte readOam() {
        return oam[oamAddr];
    }

    public void cpuWrite(int address, byte value) {
        switch (address & 0x07) {
            case 0 -> writePpuCtrl(value);
            case 1 -> writePpuMask(value);
            case 2 -> writePpuStatus(value);
            case 3 -> writeOamAddress(value);
            case 4 -> writeOamData(value);
            case 5 -> writePpuScroll(value);
            case 6 -> writePpuAddress(value);
            case 7 -> writePpuData(value);
            default ->
                throw new UnsupportedOperationException("Writing to PPU " + Util.hex4(address) + ": " + Util.hex2(value));
        }
    }

    private void writePpuCtrl(byte value) {
        ppuCtrl = Byte.toUnsignedInt(value);
    }

    private void writePpuMask(byte value) {
        ppuMask = Byte.toUnsignedInt(value);
    }

    private void writePpuStatus(byte value) {
        // Do nothing
    }

    private void writeOamAddress(byte value) {
        oamAddr = Byte.toUnsignedInt(value);
    }

    private void writeOamData(byte value) {
        oam[oamAddr] = value;
    }

    private void writePpuScroll(byte value) {
        if (!latch) {
            ppuScrollX = Byte.toUnsignedInt(value);
        } else {
            ppuScrollY = Byte.toUnsignedInt(value);
        }
        latch = !latch;
    }

    private void writePpuAddress(byte value) {
        if (!latch) {
            ppuAddrTemp = (value & 0xFF) << 8;
        } else {
            ppuAddrTemp |= value & 0xFF;
            ppuAddr = ppuAddrTemp;
        }
        latch = !latch;
    }

    private void writePpuData(byte value) {
        ppuWrite(ppuAddr, value);
        ppuAddr += (ppuCtrl & 0x04) != 0 ? 32 : 1;
    }

    private void dumpRegister(int value, String key, String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 8; i > 0; i--) {
            if ((value & (1 << (i - 1))) != 0) {
                sb.append(key.charAt(8 - i));
            } else {
                sb.append('-');
            }
        }
        System.out.println(name + " :" + sb);
    }

    public void drawBackgroundArray(byte[] pixels) {
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 32; x++) {
                renderTile(pixels, y, x);
            }
        }
    }

    private void renderTile(byte[] pixels, int y, int x) {
        int tile = ppuRead(getNameTable() + (y * 32 + x));
        int tileX = (tile % 16);
        int tileY = (tile / 16);
        for (int row = 0; row < 8; row++) {
            int offset = (tileY * 256) + (tileX * 16);
            byte tileLsb = ppuRead(getPatternTable() + (offset + row + 0));
            byte tileMsb = ppuRead(getPatternTable() + (offset + row + 8));
            for (int col = 0; col < 8; col++) {
                int shift = 7 - col;
                int msb = (tileMsb & (1 << shift)) >> shift;
                int lsb = (tileLsb & (1 << shift)) >> shift;
                int color = (msb << 1) + lsb;

                int xx = x * 8 + col;
                int yy = y * 8 + row;
                pixels[yy * 256 + xx] = palette[color];
            }
        }
    }

}
