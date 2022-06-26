package be.twofold.tinynes;

import java.util.*;

public final class Cpu {

    private static final byte[] CyclesPerInstruction = {
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

    private final Bus bus;

    int a;  // Accumulator
    int x;  // X index register
    int y;  // Y index register
    int s;  // Stack pointer
    int p;  // Status register
    int pc; // Program counter
    int cycles; // Cycles since last instruction
    boolean enabled = true;

    int totalCycles;

    public Cpu(Bus bus) {
        this.bus = Objects.requireNonNull(bus);
        reset();
    }

    public void clock() {
        if (!enabled) {
            totalCycles++;
            return;
        }
        if (cycles == 0) {
            int opcode = read(pc++);
            cycles = CyclesPerInstruction[opcode];
            execute(opcode);
        }
        cycles--;
        totalCycles++;
    }

    public void reset() {
        a = 0;
        x = 0;
        y = 0;
        s = 0xFD;
        p = 0x04;
        pc = read16(0xFFFC);
    }

    public void irq() {
        if (getI()) {
            return;
        }

        push16(pc);

        setI(true);
        push(p | 0x20);

        pc = read16(0xFFFE);

        cycles = 7;
    }

    public void nmi() {
        push16(pc);

        setI(true);
        push(p | 0x20);

        pc = read16(0xFFFA);

        cycles = 8;
    }

    // region Flags

    public boolean getC() {
        return (p & 0x01) != 0;
    }

    public boolean getZ() {
        return (p & 0x02) != 0;
    }

    public boolean getI() {
        return (p & 0x04) != 0;
    }

    public boolean getD() {
        return (p & 0x08) != 0;
    }

    public boolean getB() {
        return (p & 0x10) != 0;
    }

    public boolean getV() {
        return (p & 0x40) != 0;
    }

    public boolean getN() {
        return (p & 0x80) != 0;
    }

    public void setC(boolean value) {
        p = value ? p | 0x01 : p & 0xFE;
    }

    public void setZ(boolean value) {
        p = value ? p | 0x02 : p & 0xFD;
    }

    public void setI(boolean value) {
        p = value ? p | 0x04 : p & 0xFB;
    }

    public void setD(boolean value) {
        p = value ? p | 0x08 : p & 0xF7;
    }

    public void setV(boolean value) {
        p = value ? p | 0x40 : p & 0xBF;
    }

    public void setN(boolean value) {
        p = value ? p | 0x80 : p & 0x7F;
    }

    // endregion

    // region Helpers

    int read(int address) {
        return Byte.toUnsignedInt(bus.read(address & 0xFFFF));
    }

    int read16(int address) {
        return read16(address, address + 1);
    }

    int read16(int a1, int a2) {
        int lo = read(a1);
        int hi = read(a2);
        return (hi << 8) | lo;
    }

    private void write(int address, int value) {
        assert value >= 0 && value <= 0xFF;
        bus.write(address, (byte) value);
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
            case 0x0A -> asla();
            case 0x0D -> ora(abs());
            case 0x0E -> asl(abs());
            case 0x10 -> bpl(rel());
            case 0x11 -> ora(izy());
            case 0x15 -> ora(zpx());
            case 0x16 -> asl(zpx());
            case 0x18 -> clc();
            case 0x19 -> ora(aby());
            case 0x1D -> ora(abx());
            case 0x1E -> asl(abx_());
            case 0x20 -> jsr(abs());
            case 0x21 -> and(izx());
            case 0x24 -> bit(zp0());
            case 0x25 -> and(zp0());
            case 0x26 -> rol(zp0());
            case 0x28 -> plp();
            case 0x29 -> and(imm());
            case 0x2A -> rola();
            case 0x2C -> bit(abs());
            case 0x2D -> and(abs());
            case 0x2E -> rol(abs());
            case 0x30 -> bmi(rel());
            case 0x31 -> and(izy());
            case 0x35 -> and(zpx());
            case 0x36 -> rol(zpx());
            case 0x38 -> sec();
            case 0x39 -> and(aby());
            case 0x3D -> and(abx());
            case 0x3E -> rol(abx_());
            case 0x40 -> rti();
            case 0x41 -> eor(izx());
            case 0x45 -> eor(zp0());
            case 0x46 -> lsr(zp0());
            case 0x48 -> pha();
            case 0x49 -> eor(imm());
            case 0x4A -> lsra();
            case 0x4C -> jmp(abs());
            case 0x4D -> eor(abs());
            case 0x4E -> lsr(abs());
            case 0x50 -> bvc(rel());
            case 0x51 -> eor(izy());
            case 0x55 -> eor(zpx());
            case 0x56 -> lsr(zpx());
            case 0x58 -> cli();
            case 0x59 -> eor(aby());
            case 0x5D -> eor(abx());
            case 0x5E -> lsr(abx_());
            case 0x60 -> rts();
            case 0x61 -> adc(izx());
            case 0x65 -> adc(zp0());
            case 0x66 -> ror(zp0());
            case 0x68 -> pla();
            case 0x69 -> adc(imm());
            case 0x6A -> rora();
            case 0x6C -> jmp(ind());
            case 0x6D -> adc(abs());
            case 0x6E -> ror(abs());
            case 0x70 -> bvs(rel());
            case 0x71 -> adc(izy());
            case 0x75 -> adc(zpx());
            case 0x76 -> ror(zpx());
            case 0x78 -> sei();
            case 0x79 -> adc(aby());
            case 0x7D -> adc(abx());
            case 0x7E -> ror(abx_());
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
            case 0x91 -> sta(izy_());
            case 0x94 -> sty(zpx());
            case 0x95 -> sta(zpx());
            case 0x96 -> stx(zpy());
            case 0x98 -> tya();
            case 0x99 -> sta(aby_());
            case 0x9A -> txs();
            case 0x9D -> sta(abx_());
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
            case 0xB1 -> lda(izy());
            case 0xB4 -> ldy(zpx());
            case 0xB5 -> lda(zpx());
            case 0xB6 -> ldx(zpy());
            case 0xB8 -> clv();
            case 0xB9 -> lda(aby());
            case 0xBA -> tsx();
            case 0xBC -> ldy(abx());
            case 0xBD -> lda(abx());
            case 0xBE -> ldx(aby());
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
            case 0xD1 -> cmp(izy());
            case 0xD5 -> cmp(zpx());
            case 0xD6 -> dec(zpx());
            case 0xD8 -> cld();
            case 0xD9 -> cmp(aby());
            case 0xDD -> cmp(abx());
            case 0xDE -> dec(abx_());
            case 0xE0 -> cpx(imm());
            case 0xE1 -> sbc(izx());
            case 0xE4 -> cpx(zp0());
            case 0xE5 -> sbc(zp0());
            case 0xE6 -> inc(zp0());
            case 0xE8 -> inx();
            case 0xE9 -> sbc(imm());
            case 0xEA -> nop();
            case 0xEC -> cpx(abs());
            case 0xED -> sbc(abs());
            case 0xEE -> inc(abs());
            case 0xF0 -> beq(rel());
            case 0xF1 -> sbc(izy());
            case 0xF5 -> sbc(zpx());
            case 0xF6 -> inc(zpx());
            case 0xF8 -> sed();
            case 0xF9 -> sbc(aby());
            case 0xFD -> sbc(abx());
            case 0xFE -> inc(abx_());

            // Illegal Opcodes
            case 0x03 -> slo(izx());
            case 0x04, 0x44, 0x64 -> zp0();
            case 0x07 -> slo(zp0());
            case 0x0C -> abs();
            case 0x0F -> slo(abs());
            case 0x13 -> slo(izy());
            case 0x14, 0x34, 0x54, 0x74, 0xD4, 0xF4 -> zpx();
            case 0x17 -> slo(zpx());
            case 0x1A, 0x3A, 0x5A, 0x7A, 0xDA, 0xFA -> nop();
            case 0x1B -> slo(aby());
            case 0x1C, 0x3C, 0x5C, 0x7C, 0xDC, 0xFC -> abx();
            case 0x1F -> slo(abx());
            case 0x23 -> rla(izx());
            case 0x27 -> rla(zp0());
            case 0x2F -> rla(abs());
            case 0x33 -> rla(izy());
            case 0x37 -> rla(zpx());
            case 0x3B -> rla(aby());
            case 0x3F -> rla(abx());
            case 0x43 -> sre(izx());
            case 0x47 -> sre(zp0());
            case 0x4F -> sre(abs());
            case 0x53 -> sre(izy());
            case 0x57 -> sre(zpx());
            case 0x5B -> sre(aby());
            case 0x5F -> sre(abx());
            case 0x63 -> rra(izx());
            case 0x67 -> rra(zp0());
            case 0x6F -> rra(abs());
            case 0x73 -> rra(izy());
            case 0x77 -> rra(zpx());
            case 0x7B -> rra(aby());
            case 0x7F -> rra(abx());
            case 0x80, 0x82, 0x89 -> imm();
            case 0x83 -> sax(izx());
            case 0x87 -> sax(zp0());
            case 0x8F -> sax(abs());
            case 0x97 -> sax(zpy());
            case 0xA3 -> lax(izx());
            case 0xA7 -> lax(zp0());
            case 0xAF -> lax(abs());
            case 0xB3 -> lax(izy());
            case 0xB7 -> lax(zpy());
            case 0xBF -> lax(aby());
            case 0xC3 -> dcp(izx());
            case 0xC7 -> dcp(zp0());
            case 0xCF -> dcp(abs());
            case 0xD3 -> dcp(izy());
            case 0xD7 -> dcp(zpx());
            case 0xDB -> dcp(aby());
            case 0xDF -> dcp(abx());
            case 0xE3 -> isc(izx());
            case 0xE7 -> isc(zp0());
            case 0xEB -> sbc(imm());
            case 0xEF -> isc(abs());
            case 0xF3 -> isc(izy());
            case 0xF7 -> isc(zpx());
            case 0xFB -> isc(aby());
            case 0xFF -> isc(abx());

            default ->
                throw new IllegalArgumentException(String.format("inVALID opcode 0x%02x at 0x%04x", opcode, pc - 1));
        }
    }

    // endregion

    // region Addressing Modes

    private int imm() {
        return pc++;
    }

    private int zp0() {
        return read(imm());
    }

    private int zpx() {
        return (zp0() + x) & 0xFF;
    }

    private int zpy() {
        return (zp0() + y) & 0xFF;
    }

    private int rel() {
        byte rel = (byte) read(imm());
        return pc + rel;
    }

    private int abs() {
        int result = read16(pc);
        pc += 2;
        return result;
    }

    private int abx() {
        return cross(abs(), x);
    }

    private int abx_() {
        return (abs() + x) & 0xFFFF;
    }

    private int aby() {
        return cross(abs(), y);
    }

    private int aby_() {
        return (abs() + y) & 0xFFFF;
    }

    private int ind() {
        int i = abs();
        return read16(i, (i & 0xFF00) | ((i + 1) & 0xFF));
    }

    private int izx() {
        int i = zpx();
        return read16(i, (i + 1) & 0xFF);
    }

    private int izy() {
        int i = zp0();
        int base = read16(i, (i + 1) & 0xFF);
        return cross(base, y);
    }

    private int izy_() {
        int i = zp0();
        return read16(i, (i + 1) & 0xFF) + y;
    }

    private int cross(int base, int offset) {
        int result = base + offset;
        if ((base & 0xFF00) != (result & 0xFF00)) {
            cycles++;
        }
        return result;
    }

    // endregion

    // region Opcodes

    private void adc(int address) {
        add(a, read(address));
    }

    private void add(int v1, int v2) {
        int temp = v1 + v2 + (getC() ? 1 : 0);
        setC(temp > 0xFF);
        setV(((v1 ^ temp) & (v2 ^ temp) & 0x80) != 0);
        a = temp & 0xFF;
        setZN(a);
    }

    private void and(int address) {
        a &= read(address);
        setZN(a);
    }

    private void asl(int address) {
        write(address, asl0(read(address)));
    }

    private void asla() {
        a = asl0(a);
    }

    private int asl0(int value) {
        int temp = (value << 1) & 0xFF;
        setC((value & 0x80) != 0);
        setZN(temp);
        return temp;
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
        push16(++pc);
        push(p | 0x30);
        pc = read16(0xFFFE);
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
        cmp0(address, a);
    }

    private void cpx(int address) {
        cmp0(address, x);
    }

    private void cpy(int address) {
        cmp0(address, y);
    }

    private void cmp0(int address, int reg) {
        int fetched = read(address);
        int temp = reg - fetched;
        setC(reg >= fetched);
        setZN(temp);
    }

    private void dec(int address) {
        write(address, dec0(read(address)));
    }

    private void dex() {
        x = dec0(x);
    }

    private void dey() {
        y = dec0(y);
    }

    private int dec0(int value) {
        int temp = (value - 1) & 0xFF;
        setZN(temp);
        return temp;
    }

    private void eor(int address) {
        a ^= read(address);
        setZN(a);
    }

    private void inc(int address) {
        write(address, inc0(read(address)));
    }

    private void inx() {
        x = inc0(x);
    }

    private void iny() {
        y = inc0(y);
    }

    private int inc0(int value) {
        int temp = (value + 1) & 0xFF;
        setZN(temp);
        return temp;
    }

    private void jmp(int address) {
        pc = address;
    }

    private void jsr(int address) {
        push16(--pc);
        pc = address;
    }

    private void lda(int address) {
        a = ld0(address);
    }

    private void ldx(int address) {
        x = ld0(address);
    }

    private void ldy(int address) {
        y = ld0(address);
    }

    private int ld0(int address) {
        int temp = read(address);
        setZN(temp);
        return temp;
    }

    private void lsr(int address) {
        write(address, lsr0(read(address)));
    }

    private void lsra() {
        a = lsr0(a);
    }

    private int lsr0(int value) {
        int temp = value >>> 1;
        setC((value & 0x01) != 0);
        setZN(temp);
        return temp;
    }

    private void nop() {
    }

    private void ora(int address) {
        a |= read(address);
        setZN(a);
    }

    private void pha() {
        push(a);
    }

    private void php() {
        push(p | 0x30);
    }

    private void pla() {
        a = pop();
        setZN(a);
    }

    private void plp() {
        p = pop() & 0xCF;
    }

    private void rol(int address) {
        write(address, rol0(read(address)));
    }

    private void rola() {
        a = rol0(a);
    }

    private int rol0(int value) {
        int temp = ((value << 1) | (getC() ? 0x01 : 0x00)) & 0xFF;
        setC((value & 0x80) != 0);
        setZN(temp);
        return temp;
    }

    private void ror(int address) {
        write(address, ror0(read(address)));
    }

    private void rora() {
        a = ror0(a);
    }

    private int ror0(int value) {
        int temp = (value >>> 1) | (getC() ? 0x80 : 0x00);
        setC((value & 0x01) != 0);
        setZN(temp);
        return temp;
    }

    private void rti() {
        p = pop() & 0xCF;
        pc = pop16();
    }

    private void rts() {
        pc = pop16() + 1;
    }

    private void sbc(int address) {
        add(a, read(address) ^ 0xFF);
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
        x = s;
        setZN(x);
    }

    private void txa() {
        a = x;
        setZN(a);
    }

    private void txs() {
        s = x;
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
            pc = address;
            cycles++;
        }
    }

    private void push(int value) {
        write(0x0100 + s, value);
        s = (s - 1) & 0xFF;
    }

    private void push16(int value) {
        push((value >>> 8) & 0xFF);
        push(value & 0xFF);
    }

    private int pop() {
        s = (s + 1) & 0xFF;
        return read(0x0100 + s);
    }

    private int pop16() {
        int lo = pop();
        int hi = pop();
        return (hi << 8) | lo;
    }

    // endregion

    @Override
    public String toString() {
        return "Cpu(" +
            "a=" + Util.hex2(a) + ", " +
            "x=" + Util.hex2(x) + ", " +
            "y=" + Util.hex2(y) + ", " +
            "sp=" + Util.hex4(s) + ", " +
            "pc=" + Util.hex4(pc) +
            ")";
    }
}
