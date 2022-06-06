package be.twofold.tinynes;

import java.awt.*;
import java.awt.image.*;

public final class Ppu {

    private static final Color[] Palette = new Color[]{
        new Color(84, 84, 84), new Color(0, 30, 116), new Color(8, 16, 144), new Color(48, 0, 136),
        new Color(68, 0, 100), new Color(92, 0, 48), new Color(84, 4, 0), new Color(60, 24, 0),
        new Color(32, 42, 0), new Color(8, 58, 0), new Color(0, 64, 0), new Color(0, 60, 0),
        new Color(0, 50, 60), new Color(0, 0, 0), new Color(152, 150, 152), new Color(8, 76, 196),
        new Color(48, 50, 236), new Color(92, 30, 228), new Color(136, 20, 176), new Color(160, 20, 100),
        new Color(152, 34, 32), new Color(120, 60, 0), new Color(84, 90, 0), new Color(40, 114, 0),
        new Color(8, 124, 0), new Color(0, 118, 40), new Color(0, 102, 120), new Color(0, 0, 0),
        new Color(236, 238, 236), new Color(76, 154, 236), new Color(120, 124, 236), new Color(176, 98, 236),
        new Color(228, 84, 236), new Color(236, 88, 180), new Color(236, 106, 100), new Color(212, 136, 32),
        new Color(160, 170, 0), new Color(116, 196, 0), new Color(76, 208, 32), new Color(56, 204, 108),
        new Color(56, 180, 204), new Color(60, 60, 60), new Color(236, 238, 236), new Color(168, 204, 236),
        new Color(188, 188, 236), new Color(212, 178, 236), new Color(236, 174, 236), new Color(236, 174, 212),
        new Color(236, 180, 176), new Color(228, 196, 144), new Color(204, 210, 120), new Color(180, 222, 120),
        new Color(168, 226, 144), new Color(152, 226, 180), new Color(160, 214, 228), new Color(160, 162, 160),
    };

    private static final IndexColorModel colorModel = createColorModel();

    private static IndexColorModel createColorModel() {
        byte[] r = new byte[Palette.length];
        byte[] g = new byte[Palette.length];
        byte[] b = new byte[Palette.length];
        for (int i = 0; i < Palette.length; i++) {
            r[i] = (byte) Palette[i].getRed();
            g[i] = (byte) Palette[i].getGreen();
            b[i] = (byte) Palette[i].getBlue();
        }

        return new IndexColorModel(4, Palette.length, r, g, b);
    }

    private final Cartridge cartridge;
    private final byte[] ram = new byte[0x3fff];

    // Palette
    private final byte[][] nameTable = new byte[4][0x400];
    private final byte[][] patternTable = new byte[2][0x1000];
    private final byte[] palette = new byte[0x20];

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
    int row;
    int col;

    public Ppu(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    public void clock() {
        if (row == -1 && col == 1) {
            ppuStatus &= 0x7F; // Clear VBlank flag
        }
        if (row == 241 && col == 1) {
            ppuStatus |= 0x80; // Set VBlank flag
            if ((ppuCtrl & 0x80) != 0) {
                nmi = true;
            }
        }

        col++;
        if (col == 341) {
            col = 0;
            row++;
            if (row == 261) {
                row = -1;
            }
        }
    }

    private void ppuWrite(int address, byte data) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            patternTable[address >>> 13][address & 0x1FFF] = data;
        }
        if (address >= 0x2000 && address <= 0x3EFF) {
            address &= 0x0FFF;
            if (address >= 0x0000 && address <= 0x03FF)
                nameTable[0][address & 0x03FF] = data;
            if (address >= 0x0400 && address <= 0x07FF)
                nameTable[0][address & 0x03FF] = data;
            if (address >= 0x0800 && address <= 0x0BFF)
                nameTable[1][address & 0x03FF] = data;
            if (address >= 0x0C00 && address <= 0x0FFF)
                nameTable[1][address & 0x03FF] = data;
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
        }
    }

    public byte read(int address) {
        return (byte) switch (address & 0x07) {
            case 0 -> throw new UnsupportedOperationException();
            case 1 -> throw new UnsupportedOperationException();
            case 2 -> readPpuStatus();
            case 3 -> throw new UnsupportedOperationException();
            case 4 -> throw new UnsupportedOperationException();
            case 5 -> throw new UnsupportedOperationException();
            case 6 -> throw new UnsupportedOperationException();
            case 7 -> throw new UnsupportedOperationException();
            default -> throw new IllegalArgumentException("Invalid address: " + Util.hex4(address));
        };
    }

    private int readPpuStatus() {
        latch = false;
        return ppuStatus;
    }

    public void write(int address, byte value) {
        if (address >= 0x2002 && address <= 0x2004) {
            System.out.println("Writing to PPU " + Util.hex4(address) + ": " + Util.hex2(value));
        }
        switch (address & 0x07) {
            case 0 -> writePpuCtrl(value);
            case 1 -> writePpuMask(value);
            case 2 -> throw new UnsupportedOperationException();
            case 3 -> throw new UnsupportedOperationException();
            case 4 -> throw new UnsupportedOperationException();
            case 5 -> writePpuScroll(value);
            case 6 -> writePpuAddress(value);
            case 7 -> writePpuData(value);
            default -> throw new IllegalArgumentException("Invalid address: " + Util.hex4(address));
        }
    }

    private void writePpuCtrl(byte value) {
        ppuCtrl = Byte.toUnsignedInt(value);
        dumpRegister(ppuCtrl, "VPHBSINN", "PPUCTRL");
    }

    private void writePpuMask(byte value) {
        ppuMask = Byte.toUnsignedInt(value);
        dumpRegister(ppuMask, "BGRsbMmG", "PPUMASK");
    }

    private void writePpuScroll(byte value) {
        if (latch) {
            ppuScrollY = Byte.toUnsignedInt(value);
            latch = false;
            System.out.println("ScrollX: " + Util.hex2(ppuScrollX) + ", ScrollY: " + Util.hex2(ppuScrollY));
        } else {
            ppuScrollX = Byte.toUnsignedInt(value);
            latch = true;
        }
    }

    private void writePpuAddress(byte value) {
        if (latch) {
            ppuAddrTemp |= Byte.toUnsignedInt(value);
            ppuAddr = ppuAddrTemp;
            latch = false;
            System.out.println("PPU Address: " + Util.hex4(ppuAddr));
        } else {
            ppuAddrTemp = Byte.toUnsignedInt(value) << 8;
            latch = true;
        }
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

    BufferedImage drawBackground(int nti, int pti) {
        BufferedImage image = new BufferedImage(256, 240, BufferedImage.TYPE_BYTE_INDEXED, colorModel);

        byte[] nameTable = this.nameTable[nti];
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 32; x++) {
                int tile = nameTable[y * 32 + x];
                int tileX = tile % 16;
                int tileY = tile / 16;
                for (int row = 0; row < 8; row++) {
                    int offset = tileY * 256 + tileX * 16;
                    byte tileLsb = patternTable[pti][offset + row + 0];
                    byte tileMsb = patternTable[pti][offset + row + 8];
                    for (int col = 0; col < 8; col++) {
                        int shift = 7 - col;
                        int msb = (tileMsb & (1 << shift)) >> shift;
                        int lsb = (tileLsb & (1 << shift)) >> shift;
                        int color = msb + lsb;

                        int xx = x * 8 + col;
                        int yy = y * 8 + row;
                        image.setRGB(xx, yy, Palette[palette[color]].getRGB());
                    }
                }
            }
        }

        return image;
    }

}
