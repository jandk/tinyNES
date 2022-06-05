package be.twofold.tinynes;

import java.util.*;

public final class Cpu {

    private final Bus bus;

    private int a;      // Accumulator
    private int x;      // X index register
    private int y;      // Y index register
    private int sp;     // Stack pointer
    private int pc;     // Program counter
    private int status; // Status register

    public Cpu(Bus bus) {
        this.bus = Objects.requireNonNull(bus);
        reset();
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
        assert 0x00 <= a && a <= 0xFF;
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        assert 0x00 <= a && a <= 0xFF;
        this.y = y;
    }

    public int getSp() {
        return sp;
    }

    public void setSp(int sp) {
        assert 0x00 <= a && a <= 0xFF;
        this.sp = sp;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        assert 0x0000 <= a && a <= 0xFFFF;
        this.pc = pc;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        assert 0x00 <= a && a <= 0xFF;
        this.status = status;
    }

    // endregion

    // region Flags

    public boolean getC() {
        return (status & 0x01) != 0;
    }

    public boolean getZ() {
        return (status & 0x02) != 0;
    }

    public boolean getI() {
        return (status & 0x04) != 0;
    }

    public boolean getD() {
        return (status & 0x08) != 0;
    }

    public boolean getB() {
        return (status & 0x10) != 0;
    }

    public boolean getU() {
        return (status & 0x20) != 0;
    }

    public boolean getV() {
        return (status & 0x40) != 0;
    }

    public boolean getN() {
        return (status & 0x80) != 0;
    }

    public void setC(boolean value) {
        if (value) {
            status |= 0x01;
        } else {
            status &= 0xfe;
        }
    }

    public void setZ(boolean value) {
        if (value) {
            status |= 0x02;
        } else {
            status &= 0xfd;
        }
    }

    public void setI(boolean value) {
        if (value) {
            status |= 0x04;
        } else {
            status &= 0xfb;
        }
    }

    public void setD(boolean value) {
        if (value) {
            status |= 0x08;
        } else {
            status &= 0xf7;
        }
    }

    public void setB(boolean value) {
        if (value) {
            status |= 0x10;
        } else {
            status &= 0xef;
        }
    }

    public void setU(boolean value) {
        if (value) {
            status |= 0x20;
        } else {
            status &= 0xdf;
        }
    }

    public void setV(boolean value) {
        if (value) {
            status |= 0x40;
        } else {
            status &= 0xbf;
        }
    }

    public void setN(boolean value) {
        if (value) {
            status |= 0x80;
        } else {
            status &= 0x7f;
        }
    }

    // endregion

    public void step() {
        int opcode = nextByte();
        execute(opcode);
    }

    public void reset() {
        int lo = read(0xFFFC);
        int hi = read(0xFFFD);

        a = 0;
        x = 0;
        y = 0;
        sp = 0xFD;
        pc = (hi << 8) | lo;
        status = 0x34;
    }

    // region Decoder

    private void execute(int opcode) {
        switch (opcode) {
            // Standard Opcodes
            case 0x00 -> brk(imp());
            case 0x01 -> ora(izx());
            case 0x05 -> ora(zp0());
            case 0x06 -> asl(zp0());
            case 0x08 -> php(imp());
            case 0x09 -> ora(imm());
            case 0x0A -> asl(imp());
            case 0x0D -> ora(abs());
            case 0x0E -> asl(abs());
            case 0x10 -> bpl(rel());
            case 0x11 -> ora(izy());
            case 0x15 -> ora(zpx());
            case 0x16 -> asl(zpx());
            case 0x18 -> clc(imp());
            case 0x19 -> ora(aby());
            case 0x1D -> ora(abx());
            case 0x1E -> asl(abx());
            case 0x20 -> jsr(abs());
            case 0x21 -> and(izx());
            case 0x24 -> bit(zp0());
            case 0x25 -> and(zp0());
            case 0x26 -> rol(zp0());
            case 0x28 -> plp(imp());
            case 0x29 -> and(imm());
            case 0x2A -> rol(imp());
            case 0x2C -> bit(abs());
            case 0x2D -> and(abs());
            case 0x2E -> rol(abs());
            case 0x30 -> bmi(rel());
            case 0x31 -> and(izy());
            case 0x35 -> and(zpx());
            case 0x36 -> rol(zpx());
            case 0x38 -> sec(imp());
            case 0x39 -> and(aby());
            case 0x3D -> and(abx());
            case 0x3E -> rol(abx());
            case 0x40 -> rti(imp());
            case 0x41 -> eor(izx());
            case 0x45 -> eor(zp0());
            case 0x46 -> lsr(zp0());
            case 0x48 -> pha(imp());
            case 0x49 -> eor(imm());
            case 0x4A -> lsr(imp());
            case 0x4C -> jmp(abs());
            case 0x4D -> eor(abs());
            case 0x4E -> lsr(abs());
            case 0x50 -> bvc(rel());
            case 0x51 -> eor(izy());
            case 0x55 -> eor(zpx());
            case 0x56 -> lsr(zpx());
            case 0x58 -> cli(imp());
            case 0x59 -> eor(aby());
            case 0x5D -> eor(abx());
            case 0x5E -> lsr(abx());
            case 0x60 -> rts(imp());
            case 0x61 -> adc(izx());
            case 0x65 -> adc(zp0());
            case 0x66 -> ror(zp0());
            case 0x68 -> pla(imp());
            case 0x69 -> adc(imm());
            case 0x6A -> ror(imp());
            case 0x6C -> jmp(ind());
            case 0x6D -> adc(abs());
            case 0x6E -> ror(abs());
            case 0x70 -> bvs(rel());
            case 0x71 -> adc(izy());
            case 0x75 -> adc(zpx());
            case 0x76 -> ror(zpx());
            case 0x78 -> sei(imp());
            case 0x79 -> adc(aby());
            case 0x7D -> adc(abx());
            case 0x7E -> ror(abx());
            case 0x81 -> sta(izx());
            case 0x84 -> sty(zp0());
            case 0x85 -> sta(zp0());
            case 0x86 -> stx(zp0());
            case 0x88 -> dey(imp());
            case 0x8A -> txa(imp());
            case 0x8C -> sty(abs());
            case 0x8D -> sta(abs());
            case 0x8E -> stx(abs());
            case 0x90 -> bcc(rel());
            case 0x91 -> sta(izy());
            case 0x94 -> sty(zpx());
            case 0x95 -> sta(zpx());
            case 0x96 -> stx(zpy());
            case 0x98 -> tya(imp());
            case 0x99 -> sta(aby());
            case 0x9A -> txs(imp());
            case 0x9D -> sta(abx());
            case 0xA0 -> ldy(imm());
            case 0xA1 -> lda(izx());
            case 0xA2 -> ldx(imm());
            case 0xA4 -> ldy(zp0());
            case 0xA5 -> lda(zp0());
            case 0xA6 -> ldx(zp0());
            case 0xA8 -> tay(imp());
            case 0xA9 -> lda(imm());
            case 0xAA -> tax(imp());
            case 0xAC -> ldy(abs());
            case 0xAD -> lda(abs());
            case 0xAE -> ldx(abs());
            case 0xB0 -> bcs(rel());
            case 0xB1 -> lda(izy());
            case 0xB4 -> ldy(zpx());
            case 0xB5 -> lda(zpx());
            case 0xB6 -> ldx(zpy());
            case 0xB8 -> clv(imp());
            case 0xB9 -> lda(aby());
            case 0xBA -> tsx(imp());
            case 0xBC -> ldy(abx());
            case 0xBD -> lda(abx());
            case 0xBE -> ldx(aby());
            case 0xC0 -> cpy(imm());
            case 0xC1 -> cmp(izx());
            case 0xC4 -> cpy(zp0());
            case 0xC5 -> cmp(zp0());
            case 0xC6 -> dec(zp0());
            case 0xC8 -> iny(imp());
            case 0xC9 -> cmp(imm());
            case 0xCA -> dex(imp());
            case 0xCC -> cpy(abs());
            case 0xCD -> cmp(abs());
            case 0xCE -> dec(abs());
            case 0xD0 -> bne(rel());
            case 0xD1 -> cmp(izy());
            case 0xD5 -> cmp(zpx());
            case 0xD6 -> dec(zpx());
            case 0xD8 -> cld(imp());
            case 0xD9 -> cmp(aby());
            case 0xDD -> cmp(abx());
            case 0xDE -> dec(abx());
            case 0xE0 -> cpx(imm());
            case 0xE1 -> sbc(izx());
            case 0xE4 -> cpx(zp0());
            case 0xE5 -> sbc(zp0());
            case 0xE6 -> inc(zp0());
            case 0xE8 -> inx(imp());
            case 0xE9 -> sbc(imm());
            case 0xEA -> nop(imp());
            case 0xEC -> cpx(abs());
            case 0xED -> sbc(abs());
            case 0xEE -> inc(abs());
            case 0xF0 -> beq(rel());
            case 0xF1 -> sbc(izy());
            case 0xF5 -> sbc(zpx());
            case 0xF6 -> inc(zpx());
            case 0xF8 -> sed(imp());
            case 0xF9 -> sbc(aby());
            case 0xFD -> sbc(abx());
            case 0xFE -> inc(abx());

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
        int rel = nextByte();
        return pc + rel;
    }

    private int abs() {
        return nextWord();
    }

    private int abx() {
        int base = nextWord();
        int address = base + x;

        if ((address & 0xff00) != (base & 0xff00)) {
            // cycles++;
        }
        return address;
    }

    private int aby() {
        int base = nextWord();
        int address = base + y;

        if ((address & 0xff00) != (base & 0xff00)) {
            // cycles++;
        }
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

    private int izy() {
        int temp = nextByte();
        int lo = read((temp) & 0x00ff);
        int hi = read((temp + 1) & 0x00ff);
        int address = ((hi << 8) | lo) + y;

        if ((address & 0xff00) != (hi << 8)) {
            // cycles++;
        }
        return address;
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

    private void brk(int address) {
        throw new UnsupportedOperationException();
    }

    private void bvc(int address) {
        branch(!getV(), address);
    }

    private void bvs(int address) {
        branch(getV(), address);
    }

    private void clc(int address) {
        setC(false);
    }

    private void cld(int address) {
        setD(false);
    }

    private void cli(int address) {
        setI(false);
    }

    private void clv(int address) {
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

    private void dex(int address) {
        x = (x - 1) & 0xff;
        setZN(x);
    }

    private void dey(int address) {
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

    private void inx(int address) {
        x = (x + 1) & 0xff;
        setZN(x);
    }

    private void iny(int address) {
        y = (y + 1) & 0xff;
        setZN(y);
    }

    private void jmp(int address) {
        pc = address;
    }

    private void jsr(int address) {
        pc--;

        push((pc & 0xFF00) >>> 8);
        push(pc & 0x00FF);

        pc = address;
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

    private void pha(int address) {
        push(a);
    }

    private void php(int address) {
        push(status);
    }

    private void pla(int address) {
        a = pop();
        setZN(a);
    }

    private void plp(int address) {
        status = pop();
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

    private void rti(int address) {
        status = pop();
        setU(true); // TODO: check this
        int lo = pop();
        int hi = pop();
        pc = (hi << 8) | lo;
    }

    private void rts(int address) {
        int lo = pop();
        int hi = pop();
        pc = (hi << 8) | lo;
        pc++;
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

    private void sec(int address) {
        setC(true);
    }

    private void sed(int address) {
        setD(true);
    }

    private void sei(int address) {
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

    private void tax(int address) {
        x = a;
        setZN(x);
    }

    private void tay(int address) {
        y = a;
        setZN(y);
    }

    private void tsx(int address) {
        x = sp;
        setZN(x);
    }

    private void txa(int address) {
        a = x;
        setZN(a);
    }

    private void txs(int address) {
        sp = x;
    }

    private void tya(int address) {
        a = y;
        setZN(a);
    }

    private void setZN(int value) {
        setZ(value == 0);
        setN((value & 0x80) != 0);
    }

    private void branch(boolean condition, int address) {
        if (condition) {
            pc = address;
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

    private int read(int address) {
        if (address < 0) {
            return a;
        }
        return Byte.toUnsignedInt(bus.read(address & 0xffff));
    }

    private void write(int address, int value) {
        assert value >= 0 && value <= 0xff;
        if (address < 0) {
            a = value;
            return;
        }
        bus.write(address, (byte) value);
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

    private void incPC() {
        pc = (pc + 1) & 0xFFFF;
    }

}
