package be.twofold.tinynes;

final class Dma {
    private final Bus bus;

    private int page;
    private int counter;
    private byte data;
    private boolean skip = true;
    private boolean enabled = false;

    Dma(Bus bus) {
        this.bus = bus;
        reset();
    }

    boolean clock(int cycle) {
        if (!enabled) {
            return false;
        }
        if (skip) {
            if ((cycle & 0x01) == 1) {
                skip = false;
            }
        } else {
            if ((cycle & 0x01) == 0) {
                data = bus.read(page | counter);
            } else {
                bus.write(0x2004, data);
                if (counter++ == 0xFF) {
                    enabled = false;
                    skip = true;
                }
            }
        }
        return true;
    }

    public void start(byte value) {
        page = (value & 0xFF) << 8;
        counter = 0;
        enabled = true;
    }

    private void reset() {
        page = 0;
        counter = 0;
        enabled = false;
    }
}
