package be.twofold.tinynes;

import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.*;

class CpuTest {

    @Test
    void testWithNesTest() {
        Nes nes = load("/nestest.nes");
        nes.cpu().p = 0x04;
        nes.cpu().pc = 0xC000;
        nes.cpu().totalCycles = 7;

        List<State> states = readResults();
        for (int i = 0; i < states.size(); i++) {
            State state = states.get(i);
            assertState(nes.cpu(), state, i);
            nes.step();
        }
    }

    @Test
    void testWithBlarggInstructionTiming() {
        Nes nes = load("/instr_timing.nes");

        while (true) {
            nes.step();
            byte cpu6000 = nes.cpuBus().read(0x6000);
            if (cpu6000 != 0x00 && cpu6000 != (byte) 0x80) {
                break;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int offset = 0x6004; ; offset++) {
            int value = nes.cpuBus().read(offset);
            if (value == 0x00) {
                break;
            }
            sb.append((char) value);
        }

        System.out.println(sb);
        System.out.println(nes.cpu().totalCycles);
    }

    @Test
    void testWithBlarggCpuTimingTest() throws IOException {
        Nes nes = load("/cpu_timing_test.nes");

        List<Integer> pcs = new ArrayList<>();
        // for (int i = 0; i < 1000; i++) {
        do {
            pcs.add(nes.cpu().pc);
            nes.step();
        } while (nes.cpu().pc != 0xEA5A);

        // ImageIO.write(nes.getPpu().drawBackground(0, 0), "png", new File("C:\\Temp\\bg-0-0.png"));

        String lastPCs = pcs.subList(pcs.size() - 10, pcs.size()).stream()
            .map(Util::hex4)
            .collect(Collectors.joining(", "));

        System.out.println(lastPCs);
        System.out.println(nes.cpu().totalCycles);
    }

    private Nes load(String path) {
        InputStream in = Main.class.getResourceAsStream(path);
        Rom rom = Rom.load(in);
        Cartridge cartridge = new Cartridge(rom);
        return new Nes(cartridge);
    }

    // region NesTest

    private void assertState(Cpu cpu, State state, int i) {
        assertValue(cpu.a, state.a(), i, "A");
        assertValue(cpu.x, state.x(), i, "X");
        assertValue(cpu.y, state.y(), i, "Y");
        assertValue(cpu.s, state.sp(), i, "SP");
        assertValue(cpu.pc, state.pc(), i, "PC");
        // assertValue(cpu.totalCycles, state.cycles(), i, "cycles");

        int actual = cpu.p;
        int expected = state.status() & 0xdf; // Ignore bit 5
        assertThat(actual)
            .withFailMessage(() -> "Line " + (i + 1) + " -- Expected flags " + dumpFlags(expected) + " but was " + dumpFlags(actual) + " at " + Integer.toHexString(state.pc()))
            .isEqualTo(expected);
    }

    private void assertValue(int actual, int expected, int i, String name) {
        assertThat(actual)
            .withFailMessage(() -> "Line %d -- Expected %s to be %s but was %s"
                .formatted(i + 1, name, expected, actual))
            .isEqualTo(expected);
    }

    private static String dumpFlags(int flags) {
        return "" +
            ((flags & 0x80) != 0 ? 'N' : '-') +
            ((flags & 0x40) != 0 ? 'V' : '-') +
            ((flags & 0x20) != 0 ? 'U' : '-') +
            ((flags & 0x10) != 0 ? 'B' : '-') +
            ((flags & 0x08) != 0 ? 'D' : '-') +
            ((flags & 0x04) != 0 ? 'I' : '-') +
            ((flags & 0x02) != 0 ? 'Z' : '-') +
            ((flags & 0x01) != 0 ? 'C' : '-');
    }

    private static List<State> readResults() {
        try (InputStream in = Main.class.getResourceAsStream("/nestest.log");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
        ) {
            return readResults(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<State> readResults(BufferedReader reader) throws IOException {
        List<State> states = new ArrayList<>();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            states.add(State.parse(line));
        }
        return states;
    }

    private record State(
        int a,
        int x,
        int y,
        int sp,
        int pc,
        int status,
        int cycles
    ) {
        public static State parse(String line) {
            int a = Integer.parseInt(line.substring(50, 52), 16);
            int x = Integer.parseInt(line.substring(55, 57), 16);
            int y = Integer.parseInt(line.substring(60, 62), 16);
            int sp = Integer.parseInt(line.substring(71, 73), 16);
            int pc = Integer.parseInt(line.substring(0, 4), 16);
            int status = Integer.parseInt(line.substring(65, 67), 16);
            int cycles = Integer.parseInt(line.substring(90));
            return new State(a, x, y, sp, pc, status, cycles);
        }
    }

    // endregion

}
