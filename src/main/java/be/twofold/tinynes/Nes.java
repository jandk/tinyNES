package be.twofold.tinynes;

public final class Nes {

    private final Cpu cpu;
    private final Ppu ppu;
    private final Apu apu = new Apu();
    private final CpuBus cpuBus;
    private final PpuBus ppuBus;
    int clockCounter = 0;

    public Nes(Cartridge cartridge) {
        // Create the PPU
        ppuBus = new PpuBus(cartridge);
        ppu = new Ppu(ppuBus);

        // Create the CPU
        cpuBus = new CpuBus(cartridge, ppu, apu);
        cpu = new Cpu(cpuBus);
    }

    public Cpu cpu() {
        return cpu;
    }

    public Ppu ppu() {
        return ppu;
    }

    public Apu apu() {
        return apu;
    }

    public CpuBus cpuBus() {
        return cpuBus;
    }

    public PpuBus ppuBus() {
        return ppuBus;
    }

    public Controller controller1() {
        return cpuBus.controller1();
    }

    public Controller controller2() {
        return cpuBus.controller2();
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

    public void reset() {
        cpu.reset();
        ppu.reset();
        apu.reset();
        clockCounter = 0;
    }

}
