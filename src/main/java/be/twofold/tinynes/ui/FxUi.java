package be.twofold.tinynes.ui;

import be.twofold.tinynes.*;
import javafx.animation.*;
import javafx.application.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.*;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public class FxUi extends Application {

    private static final String Title = "TinyNES";
    private static final int Width = 256;
    private static final int Height = 240;
    private static final int Scale = 2;
    private static final Map<KeyCode, ButtonKey> KeyCodes = Map.of(
        KeyCode.X, ButtonKey.A,
        KeyCode.Z, ButtonKey.B,
        KeyCode.SHIFT, ButtonKey.SELECT,
        KeyCode.ENTER, ButtonKey.START,
        KeyCode.UP, ButtonKey.UP,
        KeyCode.DOWN, ButtonKey.DOWN,
        KeyCode.LEFT, ButtonKey.LEFT,
        KeyCode.RIGHT, ButtonKey.RIGHT
    );

    private final AnimationTimer timer = new FixedTimer(this::update);
    private final byte[] frameBuffer = new byte[Width * Height];
    private PixelBuffer<IntBuffer> pixelBuffer;
    private Stage primaryStage;
    private Canvas canvas;
    private Image image;
    private Nes nes;


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        BorderPane root = new BorderPane();
        root.setCenter(buildDisplay());
        root.setTop(buildMenu());
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(this::handleKeyPressed);
        scene.setOnKeyReleased(this::handleKeyReleased);

        primaryStage.setTitle(Title);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();

        loadRom(Path.of("src/test/resources/nestest.nes"));
        timer.start();
    }

    private void handleKeyPressed(KeyEvent event) {
        nes.cpuBus().controller1().press(KeyCodes.get(event.getCode()));
    }

    private void handleKeyReleased(KeyEvent event) {
        nes.controller1().release(KeyCodes.get(event.getCode()));
    }

    private Node buildDisplay() {
        pixelBuffer = new PixelBuffer<>(
            Width, Height, IntBuffer.allocate(Width * Height), PixelFormat.getIntArgbPreInstance());
        image = new WritableImage(pixelBuffer);

        canvas = new Canvas(Width * Scale, Height * Scale);
        canvas.getGraphicsContext2D().setImageSmoothing(false);
        return canvas;
    }

    private MenuBar buildMenu() {
        MenuItem fileLoad = new MenuItem("Load ROM");
        fileLoad.setAccelerator(KeyCombination.valueOf("Shortcut+O"));
        fileLoad.setOnAction(e -> loadRom());
        MenuItem fileQuit = new MenuItem("Quit");
        Menu file = new Menu("_File");
        file.getItems().add(fileLoad);
        file.getItems().add(new SeparatorMenuItem());
        file.getItems().add(fileQuit);

        MenuItem helpAbout = new MenuItem("About");
        Menu help = new Menu("_Help");
        help.getItems().add(helpAbout);

        MenuBar menu = new MenuBar();
        // menu.setUseSystemMenuBar(true);
        menu.getMenus().add(file);
        menu.getMenus().add(help);
        return menu;
    }

    private void update(long now) {
        if (nes != null) {
            nes.runFrame();
            nes.ppu().draw(frameBuffer);
            convertFrameBuffer(frameBuffer);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.drawImage(image, 0, 0, Width * Scale, Height * Scale);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Monospaced", 16));
            gc.setFontSmoothingType(FontSmoothingType.LCD);
            gc.fillText("FPS: " + nes.cpu().toString(), 10, 10);
        }
    }

    private void convertFrameBuffer(byte[] frameBuffer) {
        pixelBuffer.updateBuffer(pixelBuffer -> {
            int[] buffer = pixelBuffer.getBuffer().array();
            for (int i = 0; i < Width * Height; i++) {
                buffer[i] = Palette.Palette[frameBuffer[i]];
            }
            return null;
        });
    }

    public void loadRom() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a ROM");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("iNES ROM", "*.nes")
        );
        File rom = fileChooser.showOpenDialog(primaryStage);
        loadRom(rom.toPath());
    }

    private void loadRom(Path path) {
        nes = new Nes(new Cartridge(Rom.load(path)));
    }

}
