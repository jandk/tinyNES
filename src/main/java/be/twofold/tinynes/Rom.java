package be.twofold.tinynes;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class Rom {

    private final int mapperId;
    private final byte[] prg;
    private final byte[] chr;
    private final MirroringMode mirroringMode;

    public Rom(int mapperId, byte[] prg, byte[] chr, MirroringMode mirroringMode1) {
        this.mapperId = mapperId;
        this.prg = prg;
        this.chr = chr;
        this.mirroringMode = mirroringMode1;
    }

    public static Rom load(InputStream in) {
        try {
            return new Parser(in).parse();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Rom load(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int getMapperId() {
        return mapperId;
    }

    public byte[] getPrg() {
        return prg;
    }

    public byte[] getChr() {
        return chr;
    }

    public MirroringMode getMirroringMode() {
        return mirroringMode;
    }

    private static final class Parser {
        private static final byte[] Magic = {'N', 'E', 'S', 0x1A};

        private final InputStream in;

        private Parser(InputStream in) {
            this.in = in;
        }

        private Rom parse() throws IOException {
            if (!Arrays.equals(read(Magic.length), Magic)) {
                throw new IOException("Invalid magic number");
            }
            int prgSize = read();
            int chrSize = read();
            int flags6 = read();
            int flags7 = read();
            int flags8 = read();
            int flags9 = read();
            int flags10 = read();
            in.skipNBytes(5);

            MirroringMode mirroringMode = (flags6 & 0x01) == 0 ? MirroringMode.HORIZONTAL : MirroringMode.VERTICAL;

            if ((flags6 & 0x04) != 0) {
                read(0x200);
            }

            int mapperId = (flags7 & 0xF0) | (flags6 >> 4);
            byte[] prg = read(prgSize * 0x4000);
            byte[] chr = read(chrSize * 0x2000);
            return new Rom(mapperId, prg, chr, mirroringMode);
        }

        private int read() throws IOException {
            int read = in.read();
            if (read < 0) {
                throw new EOFException();
            }
            return read;
        }

        private byte[] read(int n) throws IOException {
            byte[] bytes = in.readNBytes(n);
            if (bytes.length != n) {
                throw new EOFException();
            }
            return bytes;
        }
    }

}
