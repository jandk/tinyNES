package be.twofold.tinynes;

import java.util.*;

public final class Nes {

    private final Controller controller1 = new Controller();
    private final Controller controller2 = new Controller();
    private final Cartridge cartridge;
    private final Cpu cpu;
    private final Ppu ppu;
    private final Apu apu;
    private final byte[] ram = new byte[2 * 1024];
    public int clockCounter = 0;

    public Nes(Cartridge cartridge) {
        this.cartridge = Objects.requireNonNull(cartridge);
        this.cpu = new Cpu(this);
        this.ppu = new Ppu(cartridge);
        this.apu = new Apu();
    }

    public Cpu getCpu() {
        return cpu;
    }

    public Ppu getPpu() {
        return ppu;
    }

    public Controller getController1() {
        return controller1;
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
            return ppu.cpuRead(address);
        }
        if (address >= 0x4000 && address <= 0x4018) {
            if (address <= 0x4013 || address == 0x4015) {
                return apu.read(address);
            }
            if (address == 0x4016) {
                return (byte) controller1.strobe();
            }
            if (address == 0x4017) {
                return (byte) controller2.strobe();
            }
            System.err.println("Read from IO: " + Util.hex4(address));
            return 0;
        }
        if (address >= 0x4019 && address <= 0xFFFF) {
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
            ppu.cpuWrite(address, value);
            return;
        }
        if (address >= 0x4000 && address <= 0x4018) {
            if (address <= 0x4013 || address == 0x4015 || address == 0x4017) {
                apu.write(address, value);
                return;
            }
            if (address == 0x4014) {
                oamDma(value);
                return;
            }
            if (address == 0x4016) {
                controller1.latch();
                return;
            }
            System.err.println("Write to IO: " + Util.hex4(address) + " -- " + Util.hex2(value));
            return;
        }
        if (address >= 0x4019 && address <= 0xFFFF) {
            cartridge.cpuWrite(address, value);
            return;
        }
        throw new IllegalArgumentException("Invalid address: 0x" + Integer.toHexString(address));
    }

    private void oamDma(byte value) {
        System.out.println("OAM DMA: " + Util.hex2(value));
        int bank = Byte.toUnsignedInt(value) << 8;
        cpu.enabled = false;
        for (int i = 0; i < 256; i++) {
            ppu.cpuWrite(0x2004, read(bank | i));
            for (int c = 0; c < 6; c++) {
                clock();
            }
        }
        cpu.enabled = true;
    }
}
