package be.twofold.tinynes;

import java.util.*;

public final class Cpu {

    private static final int[] CyclesPerInstruction = {
        7, 6, 0, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 0, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 0, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
        2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 0, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
        2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
        2, 6, 0, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,
        2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
        2, 5, 0, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
        2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 0, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 0, 4, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
    };

    private final Nes nes;

    private int a;  // Accumulator
    private int x;  // X index register
    private int y;  // Y index register
    private int sp; // Stack pointer
    private int pc; // Program counter
    private int st; // Status register
    int cycles; // Cycles since last instruction

    private final Tracer tracer = new Tracer();

    int totalCycles;

    public Cpu(Nes nes) {
        this.nes = Objects.requireNonNull(nes);
        reset();
    }

    public void clock() {
        if (cycles == 0) {
            int opcode = nextByte();
            cycles = CyclesPerInstruction[opcode];
            execute(opcode);
        }
        cycles--;
        totalCycles++;
    }

    public void step() {
        int opcode = nextByte();
        cycles = CyclesPerInstruction[opcode];
        execute(opcode);
        totalCycles += cycles;
    }

    public void reset() {
        int lo = read(0xFFFC);
        int hi = read(0xFFFD);

        a = 0;
        x = 0;
        y = 0;
        sp = 0xFD;
        setPc((hi << 8) | lo);
        st = 0x16;
    }

    public void irq() {
        if (getI()) {
            return;
        }

        push(pc >> 8);
        push(pc & 0xFF);

        setB(false);
        setU(true);
        setI(true);
        push(st);

        int lo = read(0xFFFE);
        int hi = read(0xFFFF);
        setPc((hi << 8) | lo);

        cycles = 7;
    }

    public void nmi() {
        push((pc >> 8) & 0xFF);
        push(pc & 0xFF);

        setB(false);
        setU(true);
        setI(true);
        push(st);

        int lo = read(0xFFFA);
        int hi = read(0xFFFB);
        setPc((hi << 8) | lo);

        cycles = 8;
    }

    // region Getters and Setters

    public int getA() {
        return a;
    }

    public void setA(int a) {
        assert 0x00 <= a && a <= 0xFF;
        this.a = a;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        assert 0x00 <= x && x <= 0xFF;
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        assert 0x00 <= y && y <= 0xFF;
        this.y = y;
    }

    public int getSp() {
        return sp;
    }

    public void setSp(int sp) {
        assert 0x00 <= sp && sp <= 0xFF;
        this.sp = sp;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        assert 0x0000 <= pc && pc <= 0xFFFF;
        this.pc = pc;
    }

    public int getSt() {
        return st;
    }

    public void setSt(int st) {
        assert 0x00 <= st && st <= 0xFF;
        this.st = st;
    }

    // endregion

    // region Flags

    public boolean getC() {
        return (st & 0x01) != 0;
    }

    public boolean getZ() {
        return (st & 0x02) != 0;
    }

    public boolean getI() {
        return (st & 0x04) != 0;
    }

    public boolean getD() {
        return (st & 0x08) != 0;
    }

    public boolean getB() {
        return (st & 0x10) != 0;
    }

    public boolean getU() {
        return (st & 0x20) != 0;
    }

    public boolean getV() {
        return (st & 0x40) != 0;
    }

    public boolean getN() {
        return (st & 0x80) != 0;
    }

    public void setC(boolean value) {
        st = value ? st | 0x01 : st & 0xfe;
    }

    public void setZ(boolean value) {
        st = value ? st | 0x02 : st & 0xfd;
    }

    public void setI(boolean value) {
        st = value ? st | 0x04 : st & 0xfb;
    }

    public void setD(boolean value) {
        st = value ? st | 0x08 : st & 0xf7;
    }

    public void setB(boolean value) {
        st = value ? st | 0x10 : st & 0xef;
    }

    public void setU(boolean value) {
        st = value ? st | 0x20 : st & 0xdf;
    }

    public void setV(boolean value) {
        st = value ? st | 0x40 : st & 0xbf;
    }

    public void setN(boolean value) {
        st = value ? st | 0x80 : st & 0x7f;
    }

    // endregion

    // region Helpers

    private void incPC() {
        pc = (pc + 1) & 0xFFFF;
    }

    int read(int address) {
        if (address < 0) {
            return a;
        }
        return Byte.toUnsignedInt(nes.read(address & 0xffff));
    }

    private void write(int address, int value) {
        assert value >= 0 && value <= 0xff;
        if (address < 0) {
            a = value;
            return;
        }
        nes.write(address, (byte) value);
    }

    private int nextByte() {
        int result = read(pc);
        incPC();
        return result;
    }

    private int nextWord() {
        int lo = read(pc);
        incPC();
        int hi = read(pc);
        incPC();
        return hi << 8 | lo;
    }

    // endregion

    // region Decoder

    private void execute(int opcode) {
        switch (opcode) {
            // Standard Opcodes
            case 0x00 -> brk();
            case 0x01 -> ora(izx());
            case 0x05 -> ora(zp0());
            case 0x06 -> asl(zp0());
            case 0x08 -> php();
            case 0x09 -> ora(imm());
            case 0x0A -> asl(imp());
            case 0x0D -> ora(abs());
            case 0x0E -> asl(abs());
            case 0x10 -> bpl(rel());
            case 0x11 -> ora(izy(opcode));
            case 0x15 -> ora(zpx());
            case 0x16 -> asl(zpx());
            case 0x18 -> clc();
            case 0x19 -> ora(aby(opcode));
            case 0x1D -> ora(abx(opcode));
            case 0x1E -> asl(abx(opcode));
            case 0x20 -> jsr(abs());
            case 0x21 -> and(izx());
            case 0x24 -> bit(zp0());
            case 0x25 -> and(zp0());
            case 0x26 -> rol(zp0());
            case 0x28 -> plp();
            case 0x29 -> and(imm());
            case 0x2A -> rol(imp());
            case 0x2C -> bit(abs());
            case 0x2D -> and(abs());
            case 0x2E -> rol(abs());
            case 0x30 -> bmi(rel());
            case 0x31 -> and(izy(opcode));
            case 0x35 -> and(zpx());
            case 0x36 -> rol(zpx());
            case 0x38 -> sec();
            case 0x39 -> and(aby(opcode));
            case 0x3D -> and(abx(opcode));
            case 0x3E -> rol(abx(opcode));
            case 0x40 -> rti();
            case 0x41 -> eor(izx());
            case 0x45 -> eor(zp0());
            case 0x46 -> lsr(zp0());
            case 0x48 -> pha();
            case 0x49 -> eor(imm());
            case 0x4A -> lsr(imp());
            case 0x4C -> jmp(abs());
            case 0x4D -> eor(abs());
            case 0x4E -> lsr(abs());
            case 0x50 -> bvc(rel());
            case 0x51 -> eor(izy(opcode));
            case 0x55 -> eor(zpx());
            case 0x56 -> lsr(zpx());
            case 0x58 -> cli();
            case 0x59 -> eor(aby(opcode));
            case 0x5D -> eor(abx(opcode));
            case 0x5E -> lsr(abx(opcode));
            case 0x60 -> rts();
            case 0x61 -> adc(izx());
            case 0x65 -> adc(zp0());
            case 0x66 -> ror(zp0());
            case 0x68 -> pla();
            case 0x69 -> adc(imm());
            case 0x6A -> ror(imp());
            case 0x6C -> jmp(ind());
            case 0x6D -> adc(abs());
            case 0x6E -> ror(abs());
            case 0x70 -> bvs(rel());
            case 0x71 -> adc(izy(opcode));
            case 0x75 -> adc(zpx());
            case 0x76 -> ror(zpx());
            case 0x78 -> sei();
            case 0x79 -> adc(aby(opcode));
            case 0x7D -> adc(abx(opcode));
            case 0x7E -> ror(abx(opcode));
            case 0x81 -> sta(izx());
            case 0x84 -> sty(zp0());
            case 0x85 -> sta(zp0());
            case 0x86 -> stx(zp0());
            case 0x88 -> dey();
            case 0x8A -> txa();
            case 0x8C -> sty(abs());
            case 0x8D -> sta(abs());
            case 0x8E -> stx(abs());
            case 0x90 -> bcc(rel());
            case 0x91 -> sta(izy(opcode));
            case 0x94 -> sty(zpx());
            case 0x95 -> sta(zpx());
            case 0x96 -> stx(zpy());
            case 0x98 -> tya();
            case 0x99 -> sta(aby(opcode));
            case 0x9A -> txs();
            case 0x9D -> sta(abx(opcode));
            case 0xA0 -> ldy(imm());
            case 0xA1 -> lda(izx());
            case 0xA2 -> ldx(imm());
            case 0xA4 -> ldy(zp0());
            case 0xA5 -> lda(zp0());
            case 0xA6 -> ldx(zp0());
            case 0xA8 -> tay();
            case 0xA9 -> lda(imm());
            case 0xAA -> tax();
            case 0xAC -> ldy(abs());
            case 0xAD -> lda(abs());
            case 0xAE -> ldx(abs());
            case 0xB0 -> bcs(rel());
            case 0xB1 -> lda(izy(opcode));
            case 0xB4 -> ldy(zpx());
            case 0xB5 -> lda(zpx());
            case 0xB6 -> ldx(zpy());
            case 0xB8 -> clv();
            case 0xB9 -> lda(aby(opcode));
            case 0xBA -> tsx();
            case 0xBC -> ldy(abx(opcode));
            case 0xBD -> lda(abx(opcode));
            case 0xBE -> ldx(aby(opcode));
            case 0xC0 -> cpy(imm());
            case 0xC1 -> cmp(izx());
            case 0xC4 -> cpy(zp0());
            case 0xC5 -> cmp(zp0());
            case 0xC6 -> dec(zp0());
            case 0xC8 -> iny();
            case 0xC9 -> cmp(imm());
            case 0xCA -> dex();
            case 0xCC -> cpy(abs());
            case 0xCD -> cmp(abs());
            case 0xCE -> dec(abs());
            case 0xD0 -> bne(rel());
            case 0xD1 -> cmp(izy(opcode));
            case 0xD5 -> cmp(zpx());
            case 0xD6 -> dec(zpx());
            case 0xD8 -> cld();
            case 0xD9 -> cmp(aby(opcode));
            case 0xDD -> cmp(abx(opcode));
            case 0xDE -> dec(abx(opcode));
            case 0xE0 -> cpx(imm());
            case 0xE1 -> sbc(izx());
            case 0xE4 -> cpx(zp0());
            case 0xE5 -> sbc(zp0());
            case 0xE6 -> inc(zp0());
            case 0xE8 -> inx();
            case 0xE9 -> sbc(imm());
            case 0xEA -> nop(imp());
            case 0xEC -> cpx(abs());
            case 0xED -> sbc(abs());
            case 0xEE -> inc(abs());
            case 0xF0 -> beq(rel());
            case 0xF1 -> sbc(izy(opcode));
            case 0xF5 -> sbc(zpx());
            case 0xF6 -> inc(zpx());
            case 0xF8 -> sed();
            case 0xF9 -> sbc(aby(opcode));
            case 0xFD -> sbc(abx(opcode));
            case 0xFE -> inc(abx(opcode));

            // Illegal Opcodes
            case 0x03 -> slo(izx());
            case 0x04 -> nop(zp0());
            case 0x07 -> slo(zp0());
            case 0x0C -> nop(abs());
            case 0x0F -> slo(abs());
            case 0x13 -> slo(izy(opcode));
            case 0x14, 0x34, 0x54, 0x74, 0xD4, 0xF4 -> nop(zpx());
            case 0x17 -> slo(zpx());
            case 0x1A, 0x3A, 0x5A, 0x7A, 0xDA, 0xFA -> nop(imp());
            case 0x1B -> slo(aby(opcode));
            case 0x1C, 0x3C, 0x5C, 0x7C, 0xDC, 0xFC -> nop(abx(opcode));
            case 0x1F -> slo(abx(opcode));
            case 0x23 -> rla(izx());
            case 0x27 -> rla(zp0());
            case 0x2F -> rla(abs());
            case 0x33 -> rla(izy(opcode));
            case 0x37 -> rla(zpx());
            case 0x3B -> rla(aby(opcode));
            case 0x3F -> rla(abx(opcode));
            case 0x43 -> sre(izx());
            case 0x44, 0x64 -> nop(zp0());
            case 0x47 -> sre(zp0());
            case 0x4F -> sre(abs());
            case 0x53 -> sre(izy(opcode));
            case 0x57 -> sre(zpx());
            case 0x5B -> sre(aby(opcode));
            case 0x5F -> sre(abx(opcode));
            case 0x63 -> rra(izx());
            case 0x67 -> rra(zp0());
            case 0x6F -> rra(abs());
            case 0x73 -> rra(izy(opcode));
            case 0x77 -> rra(zpx());
            case 0x7B -> rra(aby(opcode));
            case 0x7F -> rra(abx(opcode));
            case 0x80, 0x82, 0x89 -> nop(imm());
            case 0x83 -> sax(izx());
            case 0x87 -> sax(zp0());
            case 0x8F -> sax(abs());
            case 0x97 -> sax(zpy());
            case 0xA3 -> lax(izx());
            case 0xA7 -> lax(zp0());
            case 0xAF -> lax(abs());
            case 0xB3 -> lax(izy(opcode));
            case 0xB7 -> lax(zpy());
            case 0xBF -> lax(aby(opcode));
            case 0xC3 -> dcp(izx());
            case 0xC7 -> dcp(zp0());
            case 0xCF -> dcp(abs());
            case 0xD3 -> dcp(izy(opcode));
            case 0xD7 -> dcp(zpx());
            case 0xDB -> dcp(aby(opcode));
            case 0xDF -> dcp(abx(opcode));
            case 0xE3 -> isc(izx());
            case 0xE7 -> isc(zp0());
            case 0xEB -> sbc(imm());
            case 0xEF -> isc(abs());
            case 0xF3 -> isc(izy(opcode));
            case 0xF7 -> isc(zpx());
            case 0xFB -> isc(aby(opcode));
            case 0xFF -> isc(abx(opcode));

            default ->
                throw new IllegalArgumentException(String.format("Invalid opcode 0x%02X at 0x%04X", opcode, pc - 1));
        }
    }

    // endregion

    // region Addressing Modes

    private int imp() {
        return -1;
    }

    private int imm() {
        return pc++;
    }

    private int zp0() {
        return nextByte() & 0xff;
    }

    private int zpx() {
        return (nextByte() + x) & 0xff;
    }

    private int zpy() {
        return (nextByte() + y) & 0xff;
    }

    private int rel() {
        int rel = (byte) nextByte();
        return pc + rel;
    }

    private int abs() {
        return nextWord();
    }

    private int abx(int opcode) {
        int base = nextWord();
        int address = base + x;

        pageCrossing(address, base, opcode);
        return address;
    }

    private int aby(int opcode) {
        int base = nextWord();
        int address = base + y;

        pageCrossing(address, base, opcode);
        return address;
    }

    private int ind() {
        int base = nextWord();
        int hi = (base & 0x00ff) == 0x00ff ? base & 0xff00 : base + 1;
        return read(hi) << 8 | read(base);
    }

    private int izx() {
        int temp = nextByte();
        int lo = read((temp + x) & 0x00ff);
        int hi = read((temp + x + 1) & 0x00ff);
        return (hi << 8) | lo;
    }

    private int izy(int opcode) {
        int temp = nextByte();
        int lo = read((temp) & 0x00ff);
        int hi = read((temp + 1) & 0x00ff);
        int base = (hi << 8) | lo;
        int address = base + y;

        pageCrossing(address, base, opcode);
        return address;
    }

    Set<Integer> pageCrossings = new TreeSet<>();

    private void pageCrossing(int address, int base, int opcode) {
        pageCrossings.add(opcode);
        if ((address & 0xff00) != (base & 0xff00)) {
            int extra = switch (opcode) {
                case 0x1E, 0x3E, 0x5E, 0x7E, 0x91, 0x99, 0x9D, 0xDE, 0xFE -> 0;
                default -> 1;
            };
            cycles += extra;
        }
    }

    // endregion

    // region Opcodes

    private void adc(int address) {
        int v1 = a;
        int v2 = read(address);
        int t = v1 + v2 + (getC() ? 1 : 0);
        a = t & 0xff;
        setC(t > 0xff);
        setV(((v1 ^ t) & (v2 ^ t) & 0x80) != 0);
        setZN(a);
    }

    private void and(int address) {
        a &= read(address);
        setZN(a);
    }

    private void asl(int address) {
        int f = read(address);
        setC((f & 0x80) != 0);
        int t = (f << 1) & 0xff;
        setZN(t);
        write(address, t);
    }

    private void bcc(int address) {
        branch(!getC(), address);
    }

    private void bcs(int address) {
        branch(getC(), address);
    }

    private void beq(int address) {
        branch(getZ(), address);
    }

    private void bit(int address) {
        int f = read(address);
        setZ((a & f) == 0);
        setN((f & 0x80) != 0);
        setV((f & 0x40) != 0);
    }

    private void bmi(int address) {
        branch(getN(), address);
    }

    private void bne(int address) {
        branch(!getZ(), address);
    }

    private void bpl(int address) {
        branch(!getN(), address);
    }

    private void brk() {
        incPC();
        push(pc >> 8);
        push(pc & 0xff);
        setB(true);
        push(st);
        setB(false);
        setPc(read(0xfffe) << 8 | read(0xffff));
    }

    private void bvc(int address) {
        branch(!getV(), address);
    }

    private void bvs(int address) {
        branch(getV(), address);
    }

    private void clc() {
        setC(false);
    }

    private void cld() {
        setD(false);
    }

    private void cli() {
        setI(false);
    }

    private void clv() {
        setV(false);
    }

    private void cmp(int address) {
        int f = read(address);
        int t = a - f;
        setC(a >= f);
        setZN(t);
    }

    private void cpx(int address) {
        int f = read(address);
        int t = x - f;
        setC(x >= f);
        setZN(t);
    }

    private void cpy(int address) {
        int f = read(address);
        int t = y - f;
        setC(y >= f);
        setZN(t);
    }

    private void dec(int address) {
        int f = read(address);
        int t = (f - 1) & 0xff;
        setZN(t);
        write(address, t);
    }

    private void dex() {
        x = (x - 1) & 0xff;
        setZN(x);
    }

    private void dey() {
        y = (y - 1) & 0xff;
        setZN(y);
    }

    private void eor(int address) {
        a ^= read(address);
        setZN(a);
    }

    private void inc(int address) {
        int f = read(address);
        int t = (f + 1) & 0xff;
        setZN(t);
        write(address, t);
    }

    private void inx() {
        x = (x + 1) & 0xff;
        setZN(x);
    }

    private void iny() {
        y = (y + 1) & 0xff;
        setZN(y);
    }

    private void jmp(int address) {
        setPc(address);
    }

    private void jsr(int address) {
        pc--;

        push((pc & 0xFF00) >>> 8);
        push(pc & 0x00FF);

        setPc(address);
        tracer.jsr(address);
    }

    private void lda(int address) {
        a = read(address);
        setZN(a);
    }

    private void ldx(int address) {
        x = read(address);
        setZN(x);
    }

    private void ldy(int address) {
        y = read(address);
        setZN(y);
    }

    private void lsr(int address) {
        int f = read(address);
        setC((f & 0x01) != 0);
        int t = f >>> 1;
        setZN(t);
        write(address, t);
    }

    private void nop(int address) {
    }

    private void ora(int address) {
        a |= read(address);
        setZN(a);
    }

    private void pha() {
        push(a);
    }

    private void php() {
        push(st | 0x30);
    }

    private void pla() {
        a = pop();
        setZN(a);
    }

    private void plp() {
        st = pop();
        setB(false);
        setU(true);
    }

    private void rol(int address) {
        int f = read(address);
        int t = (f << 1) | (getC() ? 0x01 : 0x00);
        setC((f & 0x80) != 0);
        setZN(t);
        write(address, t & 0xff);
    }

    private void ror(int address) {
        int f = read(address);
        int t = (f >>> 1) | (getC() ? 0x80 : 0x00);
        setC((f & 0x01) != 0);
        setZN(t);
        write(address, t);
    }

    private void rti() {
        st = pop() & 0xcf;
        int lo = pop();
        int hi = pop();
        setPc((hi << 8) | lo);
    }

    private void rts() {
        int lo = pop();
        int hi = pop();
        setPc((hi << 8) | lo);
        incPC();
        tracer.rts(pc);
    }

    private void sbc(int address) {
        int v1 = a;
        int v2 = read(address) ^ 0xff;
        int t = v1 + v2 + (getC() ? 1 : 0);
        a = t & 0xff;
        setC(t > 0xff);
        setV(((v1 ^ t) & (v2 ^ t) & 0x80) != 0);
        setZN(a);
    }

    private void sec() {
        setC(true);
    }

    private void sed() {
        setD(true);
    }

    private void sei() {
        setI(true);
    }

    private void sta(int address) {
        write(address, a);
    }

    private void stx(int address) {
        write(address, x);
    }

    private void sty(int address) {
        write(address, y);
    }

    private void tax() {
        x = a;
        setZN(x);
    }

    private void tay() {
        y = a;
        setZN(y);
    }

    private void tsx() {
        x = sp;
        setZN(x);
    }

    private void txa() {
        a = x;
        setZN(a);
    }

    private void txs() {
        sp = x;
    }

    private void tya() {
        a = y;
        setZN(a);
    }

    // Unofficial

    private void dcp(int address) {
        dec(address);
        cmp(address);
    }

    private void isc(int address) {
        inc(address);
        sbc(address);
    }

    private void lax(int address) {
        a = x = read(address);
        setZN(a);
    }

    private void rla(int address) {
        rol(address);
        and(address);
    }

    private void rra(int address) {
        ror(address);
        adc(address);
    }

    private void sax(int address) {
        write(address, a & x);
    }

    private void sre(int address) {
        lsr(address);
        eor(address);
    }

    private void slo(int address) {
        asl(address);
        ora(address);
    }

    // Helpers

    private void setZN(int value) {
        setZ(value == 0);
        setN((value & 0x80) != 0);
    }

    private void branch(boolean condition, int address) {
        if (condition) {
            setPc(address);
            cycles++;
        }
    }

    private void push(int value) {
        write(0x0100 + sp, value);
        sp = (sp - 1) & 0xff;
    }

    private int pop() {
        sp = (sp + 1) & 0xff;
        return read(0x0100 + sp);
    }

    // endregion

    @Override
    public String toString() {
        return "Cpu(" +
            "a=" + Util.hex2(a) + ", " +
            "x=" + Util.hex2(x) + ", " +
            "y=" + Util.hex2(y) + ", " +
            "sp=" + Util.hex4(sp) + ", " +
            "pc=" + Util.hex4(pc) +
            ")";
    }
}
