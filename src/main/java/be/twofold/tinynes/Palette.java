package be.twofold.tinynes;

import java.awt.image.*;
import java.io.*;

public final class Palette {

    public static final int[] Palette = loadPalette();
    private static final IndexColorModel ColorModel = createColorModel();

    private static int[] loadPalette() {
        byte[] rawPalette;
        try (InputStream in = Ppu.class.getResourceAsStream("/YUV.pal")) {
            rawPalette = in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (rawPalette.length != (192)) {
            throw new IllegalArgumentException("Palette is not 192 bytes long");
        }

        int[] colors = new int[64];
        for (int i = 0, o = 0; i < rawPalette.length; i += 3, o++) {
            int r = rawPalette[i] & 0xff;
            int g = rawPalette[i + 1] & 0xff;
            int b = rawPalette[i + 2] & 0xff;
            colors[o] = 0xff << 24 | r << 16 | g << 8 | b;
        }
        return colors;
    }

    private static IndexColorModel createColorModel() {
        byte[] r = new byte[Palette.length];
        byte[] g = new byte[Palette.length];
        byte[] b = new byte[Palette.length];
        for (int i = 0; i < Palette.length; i++) {
            r[i] = (byte) ((Palette[i] & 0x00ff0000) >> 16);
            g[i] = (byte) ((Palette[i] & 0x0000ff00) >> 8);
            b[i] = (byte) ((Palette[i] & 0x000000ff) >> 0);
        }

        return new IndexColorModel(4, Palette.length, r, g, b);
    }

//    public static void main(String[] args) throws IOException {
//        int width = 16 * 16;
//        int height = 16 * 4;
//        int area = width * height;
//        int[] pixels = new int[area];
//        Arrays.fill(pixels, 0xff808080);
//        PixelBuffer<IntBuffer> buffer = new PixelBuffer<>(width, height, IntBuffer.wrap(pixels), PixelFormat.getIntArgbPreInstance());
//        WritableImage image = new WritableImage(buffer);
//
//        for (int y = 0; y < 4; y++) {
//            for (int x = 0; x < 16; x++) {
//                for (int yy = 1; yy < 15; yy++) {
//                    for (int xx = 1; xx < 15; xx++) {
//                        int finalX = x * 16 + xx;
//                        int finalY = y * 16 + yy;
//                        pixels[finalY * width + finalX] = Palette[y * 16 + x];
//                    }
//                }
//            }
//        }
//
//        BufferedImage oldImage = SwingFXUtils.fromFXImage(image, null);
//        ImageIO.write(oldImage, "png", new File("C:\\Temp\\palette.png"));
//    }

}
