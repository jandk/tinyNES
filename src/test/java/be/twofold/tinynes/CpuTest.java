package be.twofold.tinynes;

import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class CpuTest {

    @Test
    void testCpu() {
        Rom rom = readRom();
        Cartridge cartridge = new Cartridge(rom);
        Bus bus = new Bus(cartridge);
        Cpu cpu = new Cpu(bus);
        cpu.setPc(0xC000);

        List<State> states = readResults();
        for (int i = 0; i < states.size(); i++) {
            State state = states.get(i);
            assertState(cpu, state, i + 1);
            cpu.step();
        }
    }

    private void assertState(Cpu cpu, State state, int i) {
        assertThat(cpu.getA())
            .withFailMessage(() -> "Line " + i + " -- Expected A to be " + state.a + " but was " + cpu.getA())
            .isEqualTo(state.a());
        assertThat(cpu.getX()).isEqualTo(state.x());
        assertThat(cpu.getY()).isEqualTo(state.y());
        assertThat(cpu.getSp()).isEqualTo(state.sp());
        assertThat(cpu.getPc()).isEqualTo(state.pc());
        int status = cpu.getStatus() & 0xef; // Ignore bit 5
        assertThat(status)
            .withFailMessage(() -> "Line " + i + " -- Expected flags " + dumpFlags(state.status()) + " but was " + dumpFlags(status) + " at " + Integer.toHexString(state.pc()))
            .isEqualTo(state.status());
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

    private static Rom readRom() {
        try (InputStream in = Main.class.getResourceAsStream("/nestest.nes")) {
            return Rom.parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        int status
    ) {
        public static State parse(String line) {
            int a = Integer.parseInt(line.substring(50, 52), 16);
            int x = Integer.parseInt(line.substring(55, 57), 16);
            int y = Integer.parseInt(line.substring(60, 62), 16);
            int sp = Integer.parseInt(line.substring(71, 73), 16);
            int pc = Integer.parseInt(line.substring(0, 4), 16);
            int status = Integer.parseInt(line.substring(65, 67), 16);
            return new State(a, x, y, sp, pc, status);
        }
    }

}
