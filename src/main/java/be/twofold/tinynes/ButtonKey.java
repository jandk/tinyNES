package be.twofold.tinynes;

public enum ButtonKey {
    UP(0x10),
    DOWN(0x20),
    LEFT(0x40),
    RIGHT(0x80),
    A(0x01),
    B(0x02),
    SELECT(0x04),
    START(0x08);

    final int code;

    ButtonKey(int code) {
        this.code = code;
    }
}
