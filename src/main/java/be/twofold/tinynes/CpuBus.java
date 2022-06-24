package be.twofold.tinynes;

public final class CpuBus implements Bus {

    private final byte[] ram = new byte[2 * 1024];
    private final Cartridge cartridge;
    private final Ppu ppu;
    private final Apu apu;
    private final Controller controller1 = new Controller();
    private final Controller controller2 = new Controller();
    final Dma dma;

    public CpuBus(Cartridge cartridge, Ppu ppu, Apu apu) {
        this.cartridge = cartridge;
        this.ppu = ppu;
        this.apu = apu;
        this.dma = new Dma(this);
    }

    public Controller controller1() {
        return controller1;
    }

    public Controller controller2() {
        return controller2;
    }

    @Override
    public byte read(int address) {
        assert address >= 0x0000;

        if (address <= 0x1FFF) {
            return ram[address & 0x07FF];
        }

        if (address <= 0x3FFF) {
            return ppu.cpuRead(address);
        }

        if (address <= 0x401F) {
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

        if (address <= 0xFFFF) {
            return cartridge.cpuRead(address);
        }

        throw new IllegalArgumentException("Illegal CPU read: $" + Integer.toHexString(address));
    }

    @Override
    public void write(int address, byte value) {
        assert address >= 0x0000;

        if (address <= 0x1FFF) {
            ram[address & 0x07FF] = value;
            return;
        }

        if (address <= 0x3FFF) {
            ppu.cpuWrite(address, value);
            return;
        }

        if (address <= 0x401F) {
            if (address <= 0x4013 || address == 0x4015 || address == 0x4017) {
                apu.write(address, value);
                return;
            }
            if (address == 0x4014) {
                dma.start(value);
                return;
            }
            if (address == 0x4016) {
                if ((value & 0x01) != 0) {
                    controller1.latch();
                    controller2.latch();
                }
                return;
            }
            System.err.println("Write to IO: " + Util.hex4(address) + " -- " + Util.hex2(value));
            return;
        }

        if (address <= 0xFFFF) {
            cartridge.cpuWrite(address, value);
            return;
        }

        throw new IllegalArgumentException("Illegal CPU write: $" + Integer.toHexString(address));
    }

}
