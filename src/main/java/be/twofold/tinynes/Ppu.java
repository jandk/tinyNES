package be.twofold.tinynes;

public final class Ppu {

    private final Cartridge cartridge;

    // Palette
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

    // Running counters
    boolean nmi;
    int row = 0;
    int col = 0;

    public Ppu(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    // region Flags

    // PPUCTRL

    public int nameTable() {
        return 0x2000 + ((ppuCtrl & 0x03) << 10);
    }

    public int addressIncrement() {
        return (ppuCtrl & 0x04) != 0 ? 32 : 1;
    }

    public int spriteTable() {
        return (ppuCtrl & 0x08) << 9;
    }

    public int backgroundTable() {
        return (ppuCtrl & 0x10) << 8;
    }

    public int spriteSize() {
        return (ppuCtrl & 0x20) != 0 ? 16 : 8;
    }

    public boolean ppuMasterSlave() {
        return (ppuCtrl & 0x40) != 0;
    }

    public boolean nmi() {
        return (ppuCtrl & 0x80) != 0;
    }


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

    public void verticalBlank(boolean value) {
        ppuStatus = (byte) ((ppuStatus & 0x7F) | (value ? 0x80 : 0x00));
    }

    public boolean spriteZeroHit() {
        return (ppuStatus & 0x40) != 0;
    }

    public void spriteZeroHit(boolean value) {
        ppuStatus = (byte) ((ppuStatus & 0xBF) | (value ? 0x40 : 0x00));
    }

    public boolean spriteOverflow() {
        return (ppuStatus & 0x20) != 0;
    }

    public void spriteOverflow(boolean value) {
        ppuStatus = (byte) ((ppuStatus & 0xDF) | (value ? 0x20 : 0x00));
    }

    // endregion

    public void clock() {
        if (col == 1) {
            if (row == 241) {
                verticalBlank(true); // Set VBlank flag
                if (nmi()) {
                    nmi = true;
                }
            }
            if (row == 261) {
                verticalBlank(false); // Clear VBlank flag
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

    private int ppuRead(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            return Byte.toUnsignedInt(cartridge.ppuRead(address));
        }
        if (address >= 0x2000 && address <= 0x3FFF) {
            return Byte.toUnsignedInt(nameTable[address & 0x0FFF]);
        }
        throw new IllegalArgumentException("Illegal PPU read: " + Util.hex4(address));
    }

    private void ppuWrite(int address, byte data) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            cartridge.ppuWrite(address, data);
            return;
        }
        if (address >= 0x2000 && address <= 0x3EFF) {
            nameTable[address & 0x0FFF] = data;
            return;
        }
        if (address >= 0X3F00 && address <= 0X3FFF) {
            address &= 0x1F;
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
            case 7 -> readPpuData();
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

    private byte readPpuData() {
        byte result = (byte) ppuRead(ppuAddr);
        ppuAddr += addressIncrement();
        return result;
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
        oamAddr = (oamAddr + addressIncrement()) & 0xFF;
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
        ppuAddr += addressIncrement();
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

    public void draw(byte[] screen) {
        if (renderBackground()) {
            drawBackground(screen);
        }
        if (renderSprites()) {
            drawSprites(screen);
        }
    }

    public void drawBackground(byte[] pixels) {
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 32; x++) {
                renderTile(pixels, y, x);
            }
        }
    }

    private void renderTile(byte[] pixels, int y, int x) {
        int tile = ppuRead(nameTable() + y * 32 + x);
        int tileX = (tile % 16);
        int tileY = (tile / 16);
        int offset = (tileY * 256) + (tileX * 16);
        int paletteAddress = nameTable() | 0x03C0 | ((y >> 2) << 3) | (x >> 2);
        int pi = ppuRead(paletteAddress);
        pi >>= (y & 0x02) << 1;
        pi >>= (x & 0x02);
        pi &= 0x03;
        for (int r = 0, ro = y * (8 * 256); r < 8; r++, ro += 256) {
            int tileLsb = ppuRead(backgroundTable() + (offset + r + 0));
            int tileMsb = ppuRead(backgroundTable() + (offset + r + 8));
            int tileInt = interleave(tileLsb, tileMsb);
            for (int c = 0, co = x * 8; c < 8; c++, co++) {
                int colorIndex = (tileInt >> 14 - (c * 2)) & 0x03;
                pixels[ro + co] = palette[pi * 4 + colorIndex];
            }
        }
    }

    public void drawSprites(byte[] pixels) {
        for (int i = 0; i < 256; i += 4) {
            int y = Byte.toUnsignedInt(oam[i]) + 1;
            int index = Byte.toUnsignedInt(oam[i + 1]);
            int attr = Byte.toUnsignedInt(oam[i + 2]);
            int x = Byte.toUnsignedInt(oam[i + 3]);

            renderSprite(pixels, x, y, index, attr);
        }
    }

    private void renderSprite(byte[] pixels, int x, int y, int index, int attr) {
        if (x + 8 > 256 || y + 8 > 240) {
            return;
        }

        int offset = spriteTable() + (index * 16);
        int pi = (attr & 0x03) + 4;

        boolean flipX = (attr & 0x40) != 0;
        boolean flipY = (attr & 0x80) != 0;

        for (int r = 0; r < 8; r++) {
            int rr = flipY ? 7 - r : r;
            int ro = (y + rr) * 256;
            int tileLsb = ppuRead(offset + rr + 0);
            int tileMsb = ppuRead(offset + rr + 8);
            int tileInt = interleave(tileLsb, tileMsb);
            for (int c = 0; c < 8; c++) {
                int cc = flipX ? 7 - c : c;
                int colorIndex = (tileInt >> 14 - (cc * 2)) & 0x03;
                if (colorIndex != 0) {
                    pixels[ro + x + c] = palette[pi * 4 + colorIndex];
                }
            }
        }
    }

    private int interleave(int lsb, int msb) {
        long lsbI = ((lsb * 0x0101010101010101L & 0x8040201008040201L) * 0x0102040810204081L >> 49) & 0x5555;
        long msbI = ((msb * 0x0101010101010101L & 0x8040201008040201L) * 0x0102040810204081L >> 48) & 0xAAAA;
        return (int) (lsbI | msbI);
    }
}
