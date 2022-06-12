package be.twofold.tinynes;

import java.util.*;

public final class Nes {

    private final Cartridge cartridge;
    private final Cpu cpu;
    private final Ppu ppu;
    private final byte[] ram = new byte[2 * 1024];
    public int clockCounter = 0;

    public Nes(Cartridge cartridge) {
        this.cartridge = Objects.requireNonNull(cartridge);
        this.cpu = new Cpu(this);
        this.ppu = new Ppu(cartridge);
    }

    public Cpu getCpu() {
        return cpu;
    }

    public Ppu getPpu() {
        return ppu;
    }

    public void clock() {
        ppu.clock();
        if ((clockCounter % 3) == 0) {
            cpu.clock();
        }
        if (ppu.nmi) {
            ppu.nmi = false;
            cpu.nmi();
        }
        clockCounter++;
    }

    public void step() {
        for (int i = 0; i < 3; i++) {
            clock();
        }
        while (cpu.cycles != 0) {
            clock();
        }
    }

    public void runFrame() {
        clock();
        while (ppu.row != 0 || ppu.col != 0) {
            clock();
        }
    }

    public byte read(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            return ram[address & 0x07FF];
        }
        if (address >= 0x2000 && address <= 0x3FFF) {
            // System.err.println("Read from PPU: " + Util.hex4(address));
            return ppu.read(address);
        }
        if (address >= 0x4000 && address <= 0x401F) {
            if (address <= 0x4013 || address == 0x4015 || address == 0x4017) {
                // System.err.println("Read from APU: " + Util.hex4(address));
                return 0;
            }
            // System.err.println("Read from IO: " + Util.hex4(address));
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
            ppu.write(address, value);
            return;
        }
        if (address >= 0x4000 && address <= 0x401F) {
            if (address <= 0x4013 || address == 0x4015 || address == 0x4017) {
                // System.err.println("Write to APU: " + Util.hex4(address) + " -- " + Util.hex2(value));
                return;
            }
            // System.err.println("Write to IO: " + Util.hex4(address) + " -- " + Util.hex2(value));
            return;
        }
        if (address >= 0x4020 && address <= 0xFFFF) {
            cartridge.cpuWrite(address, value);
            return;
        }
        throw new IllegalArgumentException("Invalid address: 0x" + Integer.toHexString(address));
    }
}
