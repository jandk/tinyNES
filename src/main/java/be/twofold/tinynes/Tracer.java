package be.twofold.tinynes;

public final class Tracer {

    private int indent = 0;

    public void jsr(int address) {
//        System.out.println(" ".repeat(indent) + "jsr " + Util.hex4(address));
        indent++;
    }

    public void rts(int address) {
        indent--;
//        System.out.println(" ".repeat(indent) + "rts " + Util.hex4(address));
    }

}
