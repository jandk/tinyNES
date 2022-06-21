package be.twofold.tinynes;

public final class Controller {

    private int state;
    private int latch;

    public Controller() {
    }

    public void press(ButtonKey button) {
        if (button != null) {
            state |= button.code;
        }
    }

    public void release(ButtonKey button) {
        if (button != null) {
            state &= ~button.code;
        }
    }

    public void latch() {
        latch = state;
    }

    public int strobe() {
        int output = latch & 1;
        latch = ((latch >> 1) | 0x100);
        return output;
    }

}
