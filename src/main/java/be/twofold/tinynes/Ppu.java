package be.twofold.tinynes;

public final class Ppu {

    // Palette
    private final byte[] oam = new byte[0x100];
    private final PpuBus bus;

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

    public Ppu(PpuBus bus) {
        this.bus = bus;
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

    public byte cpuRead(int address) {
        int register = address & 0x07;

        return (byte) switch (register) {
            case 2 -> {
                latch = false;
                yield ppuStatus;
            }
            case 4 -> oam[oamAddr];
            case 7 -> {
                byte result = bus.read(ppuAddr);
                ppuAddr += addressIncrement();
                yield result;
            }
            default -> throw new IllegalArgumentException("Invalid register: " + (register));
        };
    }

    public void cpuWrite(int address, byte data) {
        int register = address & 0x07;
        int value = Byte.toUnsignedInt(data);

        switch (register) {
            case 0 -> ppuCtrl = value;
            case 1 -> ppuMask = value;
            case 2 -> {
            }
            case 3 -> oamAddr = value;
            case 4 -> {
                oam[oamAddr] = data;
                oamAddr = (oamAddr + 1) & 0xFF;
            }
            case 5 -> {
                if (!latch) {
                    ppuScrollX = Byte.toUnsignedInt(data);
                } else {
                    ppuScrollY = Byte.toUnsignedInt(data);
                }
                latch = !latch;
            }
            case 6 -> {
                if (!latch) {
                    ppuAddrTemp = (data & 0xFF) << 8;
                } else {
                    ppuAddrTemp |= data & 0xFF;
                    ppuAddr = ppuAddrTemp;
                }
                latch = !latch;
            }
            case 7 -> writePpuData(data);
            default ->
                throw new UnsupportedOperationException("Writing to PPU " + Util.hex4(address) + ": " + Util.hex2(data));
        }
    }

    private void writePpuData(byte value) {
        bus.write(ppuAddr, value);
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

    public void reset() {
    }

    private void drawBackground(byte[] pixels) {
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 32; x++) {
                renderTile(pixels, y, x);
            }
        }
    }

    private void renderTile(byte[] pixels, int y, int x) {
        int tile = Byte.toUnsignedInt(bus.read(nameTable() + y * 32 + x));
        int tileX = (tile % 16);
        int tileY = (tile / 16);
        int offset = (tileY * 256) + (tileX * 16);
        int paletteAddress = nameTable() | 0x03C0 | ((y >> 2) << 3) | (x >> 2);
        int pi = Byte.toUnsignedInt(bus.read(paletteAddress));
        pi >>= (y & 0x02) << 1;
        pi >>= (x & 0x02);
        pi &= 0x03;
        for (int r = 0, ro = y * (8 * 256); r < 8; r++, ro += 256) {
            int tileLsb = Byte.toUnsignedInt(bus.read(backgroundTable() + (offset + r + 0)));
            int tileMsb = Byte.toUnsignedInt(bus.read(backgroundTable() + (offset + r + 8)));
            int tileInt = interleave(tileLsb, tileMsb);
            for (int c = 0, co = x * 8; c < 8; c++, co++) {
                int colorIndex = (tileInt >> 14 - (c * 2)) & 0x03;
                pixels[ro + co] = bus.palette[pi * 4 + colorIndex];
            }
        }
    }

    private void drawSprites(byte[] pixels) {
        for (int i = 63; i >= 0; i--) {
            int i4 = i * 4;
            int y = Byte.toUnsignedInt(oam[i4]) + 1;
            int index = Byte.toUnsignedInt(oam[i4 + 1]);
            int attr = Byte.toUnsignedInt(oam[i4 + 2]);
            int x = Byte.toUnsignedInt(oam[i4 + 3]);

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
            int tileLsb = Byte.toUnsignedInt(bus.read(offset + rr + 0));
            int tileMsb = Byte.toUnsignedInt(bus.read(offset + rr + 8));
            int tileInt = interleave(tileLsb, tileMsb);
            for (int c = 0; c < 8; c++) {
                int cc = flipX ? 7 - c : c;
                int colorIndex = (tileInt >> 14 - (cc * 2)) & 0x03;
                if (colorIndex != 0) {
                    pixels[ro + x + c] = bus.palette[pi * 4 + colorIndex];
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
