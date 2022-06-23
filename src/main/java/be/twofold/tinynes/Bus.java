package be.twofold.tinynes;

public interface Bus {

    byte read(int address);

    void write(int address, byte value);

}
