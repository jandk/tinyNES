package be.twofold.tinynes;

import java.util.*;

public final class Ppu {

    // Palette
    private final byte[] frame = new byte[256 * 240];
    private final byte[] oam = new byte[0x100];
    private final PpuBus bus;

    // PPU Address Logic
    private boolean latch;
    private boolean frameIsOdd;

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
        return 8 << ((ppuCtrl & 0x20) >> 5);
    }

    public boolean nmi() {
        return (ppuCtrl & 0x80) != 0;
    }


    // PPUMASK

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

    private boolean rendering() {
        return (ppuMask & 0x18) != 0;
    }


    // PPUSTATUS

    public void verticalBlank(boolean value) {
        ppuStatus = (ppuStatus & ~0x80) | (value ? 0x80 : 0);
    }

    public void spriteZeroHit(boolean value) {
        ppuStatus = (ppuStatus & ~0x40) | (value ? 0x40 : 0);
    }

    public void spriteOverflow(boolean value) {
        ppuStatus = (ppuStatus & ~0x20) | (value ? 0x20 : 0);
    }

    // endregion

    // region PPU Registers

    private int v;
    private int t;
    private int x;

    private int coarseX() {
        return v & 0x1F;
    }

    private int coarseY() {
        return (v >>> 5) & 0x1F;
    }

    public int fineY() {
        return (v >>> 12) & 0x07;
    }

    private void tempNameTable(int value) {
        assert value >= 0 && value <= 3;
        t = (t & 0xF3FF) | (value << 10);
    }

    // endregion

    public void clock() {
        if (row >= 0 && row < 240 || row == 261) {
            // Sprites
            switch (col) {
                case 257 -> filterSprites();
                case 340 -> prepareSprites();
            }

            // Background
            updateBackground();
        }

        if (row == 241)
            if (col == 1) {
                verticalBlank(true);

                if (nmi()) {
                    nmi = true;
                }
            }

        if (row == 261) {
            if (col == 1) {
                verticalBlank(false);
                spriteOverflow(false);
                spriteZeroHit(false);
            }
            if (col >= 280 && col < 305) {
                transferY();
            }
        }

        if (++col > 340) {
            col = 0;
            if (++row > 261) {
                row = 0;
                frameIsOdd = !frameIsOdd;
            }
        }
    }

    // region Sprites

    private final byte[] spriteScanline = new byte[32];
    private final int[] spritePixels = new int[8];
    private final int[] spriteIds = new int[8];

    private void filterSprites() {
        Arrays.fill(spriteScanline, (byte) 0xFF);
        Arrays.fill(spritePixels, 0);
        if (row == 261) return;

        int spriteCount = 0;
        int spriteSize = spriteSize();
        for (int i = 0; i < 64; i++) {
            int line = row - getSpriteY(oam, i);

            if (line >= 0 && line < spriteSize) {
                System.arraycopy(oam, i * 4, spriteScanline, spriteCount * 4, 4);
                spriteIds[spriteCount] = i;

                if (++spriteCount >= 8) {
                    spriteOverflow(true);
                    break;
                }
            }
        }
    }

    private void prepareSprites() {
        for (int i = 0; i < 8; i++) {
            int spriteId = getSpriteId(spriteScanline, i);
            int spriteSize = spriteSize();
            int spriteAddress = spriteSize == 8
                ? spriteTable() | spriteId << 4
                : (spriteId & 0x01) << 12 | (spriteId & 0xFE) << 4;

            int spriteY = (row - getSpriteY(spriteScanline, i)) % spriteSize;
            if ((getSpriteAttr(spriteScanline, i) & 0x80) != 0) {
                spriteY ^= spriteSize - 1;
            }

            int address = (spriteAddress | spriteY & 0x08) + spriteY;
            spritePixels[i] = interleave(read(address), read(address + 8));
        }
    }

    private int getSpriteX(byte[] table, int index) {
        return Byte.toUnsignedInt(table[index * 4 + 3]);
    }

    private int getSpriteY(byte[] table, int index) {
        return Byte.toUnsignedInt(table[index * 4]);
    }

    private int getSpriteId(byte[] table, int index) {
        return Byte.toUnsignedInt(table[index * 4 + 1]);
    }

    private int getSpriteAttr(byte[] table, int index) {
        return table[index * 4 + 2];
    }

    // endregion

    // region Background

    private int bgNextId;
    private int bgNextAttr;
    private int bgNextAddr;
    private int bgNextLsb;
    private int bgNextMsb;
    private int bgShifter;
    private int bgAttrShifter;

    private void updateBackground() {
        if ((col >= 1 && col <= 257) || (col >= 321 && col <= 336)) {
            updateShifters();

            switch ((col - 1) & 0x07) {
                case 0 -> {
                    loadBackgroundShifters();
                    bgNextId = read(0x2000 | (v & 0x0FFF));
                }

                case 2 -> {
                    int x = coarseX();
                    int y = coarseY();

                    bgNextAttr = read(0x23c0 | v & 0x0C00 | ((y >> 2) << 3) | (x >> 2));
                    bgNextAttr >>= (y & 0x02) << 1;
                    bgNextAttr >>= (x & 0x02);
                    bgNextAttr &= 0x03;
                }

                case 4 -> {
                    bgNextAddr = backgroundTable() | (bgNextId << 4) | fineY();
                    bgNextLsb = read(bgNextAddr);
                }

                case 6 -> bgNextMsb = read(bgNextAddr + 8);

                case 7 -> incrementX();
            }
        }

        if (col == 256) {
            incrementY();
        }

        if (col == 257) {
            loadBackgroundShifters();
            transferX();
        }

        if (col == 338 || col == 340) {
            bgNextId = read(0x2000 | (v & 0x0FFF));
        }

        renderPixel();
    }

    private void loadBackgroundShifters() {
        bgShifter = (bgShifter & 0xFFFF0000) | interleave(bgNextLsb, bgNextMsb);
        bgAttrShifter = (bgAttrShifter & 0xFFFF0000) | (bgNextAttr * 0x5555);
    }

    private void updateShifters() {
        if (renderBackground()) {
            bgShifter <<= 2;
            bgAttrShifter <<= 2;
        }
    }

    private void incrementX() {
        if (!rendering()) {
            return;
        }

        if ((v & 0x001F) == 31) {
            v &= ~0x001F;
            v ^= 0x0400;
            return;
        }

        v += 1;
    }

    private void incrementY() {
        if (!rendering()) {
            return;
        }

        if ((v & 0x7000) != 0x7000) {
            v += 0x1000;
            return;
        }

        v &= ~0x7000;
        int y = coarseY();
        if (y == 29) {
            y = 0;
            v ^= 0x0800;
        } else if (y == 31) {
            y = 0;
        } else {
            y++;
        }
        v = (v & ~0x03E0) | (y << 5);
    }

    private void transferX() {
        if (!rendering()) {
            return;
        }
        v = (v & 0x7BE0) | (t & 0x041F);
    }

    private void transferY() {
        if (!rendering()) {
            return;
        }
        v = (v & 0x041F) | (t & 0x7BE0);
    }

    // endregion

    private void renderPixel() {
        int px = col - 1;
        int py = row;
        if (py < 0 || py >= 240 || px < 0 || px >= 256) {
            return;
        }

        int bgPalette = 0;
        if (renderBackground() && (renderBackgroundLeft() || px >= 8)) {
            int shift = 30 - (x << 1);
            bgPalette = (bgShifter >>> shift) & 0x03;
            if (bgPalette != 0) {
                bgPalette |= ((bgAttrShifter >>> shift) & 0x03) << 2;
            }
        }

        int fgPalette = 0;
        boolean fgPriority = false;
        if (renderSprites() && (renderSpritesLeft() || px >= 8)) {

            for (int i = 0; i < 8; i++) {
                int spriteAttr = getSpriteAttr(spriteScanline, i);
                int spriteX = px - getSpriteX(spriteScanline, i);
                if (spriteX < 0 || spriteX >= 8) continue;

                if ((spriteAttr & 0x40) != 0) spriteX ^= 7;
                fgPalette = (spritePixels[i] >>> ((7 - spriteX) << 1)) & 0x03;
                if (fgPalette == 0) continue;

                if (spriteIds[i] == 0 && bgPalette != 0) spriteZeroHit(true);
                fgPalette |= ((spriteAttr & 0x03) | 0x04) << 2;
                fgPriority = (spriteAttr & 0x20) == 0;
                break;
            }
        }

        int palette = priority(bgPalette, fgPalette, fgPriority);
        frame[(py * 256) + px] = bus.read(0x3F00 | palette);
    }

    private int priority(int bgPalette, int fgPalette, boolean fgPriority) {
        if (fgPalette == 0) return bgPalette;
        if (bgPalette == 0) return fgPalette;
        return fgPriority ? fgPalette : bgPalette;
    }

    public byte cpuRead(int address) {
        int register = address & 0x07;

        return switch (register) {
            case 2 -> readPpuStatus();
            case 4 -> oam[oamAddr];
            case 7 -> readPpuData();
            default -> throw new IllegalArgumentException("Invalid register: " + (register));
        };
    }

    private byte readPpuStatus() {
        byte result = (byte) (ppuStatus & 0xE0);
        latch = false;
        verticalBlank(false);
        return result;
    }

    private byte ppuDataBuffer;

    private byte readPpuData() {
        byte result = ppuDataBuffer;
        ppuDataBuffer = bus.read(v);
        v += addressIncrement();
        return v >= 0x3F00 ? ppuDataBuffer : result;
    }

    public void cpuWrite(int address, byte data) {
        int register = address & 0x07;
        int value = Byte.toUnsignedInt(data);

        switch (register) {
            case 0 -> {
                ppuCtrl = value;
                tempNameTable(value & 0x03);
            }
            case 1 -> ppuMask = value;
            case 2 -> {
            }
            case 3 -> oamAddr = value;
            case 4 -> {
                oam[oamAddr] = data;
                oamAddr = (oamAddr + 1) & 0xFF;
            }
            case 5 -> writePpuScroll(data);
            case 6 -> writePpuAddr(data);
            case 7 -> {
                bus.write(v, data);
                v += addressIncrement();
            }
            default ->
                throw new UnsupportedOperationException("Writing to PPU " + Util.hex4(address) + ": " + Util.hex2(data));
        }
    }

    private void writePpuScroll(byte value) {
        int data = Byte.toUnsignedInt(value);
        if (!latch) {
            x = data & 0x07;
            t = t & 0x7FE0 | (data >>> 3);
        } else {
            t = t & 0x0C1F | (data & 0x07) << 12 | data >>> 3 << 5;
        }
        latch = !latch;
    }

    private void writePpuAddr(byte value) {
        if (!latch) {
            t = (t & 0x00FF) | ((value & 0x3F) << 8);
        } else {
            t = (t & 0x7F00) | (value & 0xFF);
            v = t;
        }
        latch = !latch;
    }

    public void draw(byte[] screen) {
        System.arraycopy(frame, 0, screen, 0, screen.length);
    }

    public void reset() {
    }

    // region Old Helpers

    private int interleave(int lsb, int msb) {
        long lsbI = ((lsb * 0x0101010101010101L & 0x8040201008040201L) * 0x0102040810204081L >> 49) & 0x5555;
        long msbI = ((msb * 0x0101010101010101L & 0x8040201008040201L) * 0x0102040810204081L >> 48) & 0xAAAA;
        return (int) (lsbI | msbI);
    }

    // endregion

    private int read(int address) {
        return Byte.toUnsignedInt(bus.read(address));
    }
}
