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
        cpu.setSp(0xC000);

        List<Result> results = readResults();
        for (Result result : results) {
            cpu.step();
            assertResult(cpu, result);
        }
    }

    private void assertResult(Cpu cpu, Result result) {
        assertThat(cpu.getA()).isEqualTo(result.a());
        assertThat(cpu.getX()).isEqualTo(result.x());
        assertThat(cpu.getY()).isEqualTo(result.y());
        assertThat(cpu.getSp()).isEqualTo(result.sp());
        assertThat(cpu.getPc()).isEqualTo(result.pc());
        assertThat(cpu.getStatus()).isEqualTo(result.status());
    }

    private static String dumpFlags(Cpu cpu) {
        return "Flags: " +
            (cpu.getC() ? 'C' : '-') +
            (cpu.getZ() ? 'Z' : '-') +
            (cpu.getI() ? 'I' : '-') +
            (cpu.getD() ? 'D' : '-') +
            (cpu.getB() ? 'B' : '-') +
            (cpu.getU() ? 'U' : '-') +
            (cpu.getV() ? 'V' : '-') +
            (cpu.getN() ? 'N' : '-');
    }

    private static Rom readRom() {
        try (InputStream in = Main.class.getResourceAsStream("/nestest.nes")) {
            return Rom.parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Result> readResults() {
        try (InputStream in = Main.class.getResourceAsStream("/nestest.log");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
        ) {
            return readResults(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Result> readResults(BufferedReader reader) throws IOException {
        List<Result> results = new ArrayList<>();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            results.add(Result.parse(line));
        }
        return results;
    }

    private record Result(
        int a,
        int x,
        int y,
        int sp,
        int pc,
        int status
    ) {
        public static Result parse(String line) {
            int a = Integer.parseInt(line.substring(50, 52), 16);
            int x = Integer.parseInt(line.substring(55, 57), 16);
            int y = Integer.parseInt(line.substring(60, 62), 16);
            int sp = Integer.parseInt(line.substring(71, 73), 16);
            int pc = Integer.parseInt(line.substring(0, 4), 16);
            int status = Integer.parseInt(line.substring(65, 67), 16);
            return new Result(a, x, y, sp, pc, status);
        }
    }

}
