package be.twofold.tinynes;

import java.io.*;
import java.util.*;

public final class Rom {

    private final int mapperId;
    private final byte[] prg;
    private final byte[] chr;

    public Rom(int mapperId, byte[] prg, byte[] chr) {
        this.mapperId = mapperId;
        this.prg = prg;
        this.chr = chr;
    }

    public static Rom parse(InputStream in) {
        try {
            return new Parser(in).parse();
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

            if ((flags6 & 0x04) != 0) {
                read(0x200);
            }

            int mapperId = (flags7 & 0xF0) | (flags6 >> 4);
            byte[] prg = read(prgSize * 0x4000);
            byte[] chr = read(chrSize * 0x2000);
            return new Rom(mapperId, prg, chr);
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
