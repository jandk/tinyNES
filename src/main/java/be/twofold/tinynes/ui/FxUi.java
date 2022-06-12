package be.twofold.tinynes.ui;

import javafx.animation.*;
import javafx.application.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;

public class FxUi extends Application {

    private static final String Title = "TinyNES";
    private static final int Width = 256;
    private static final int Height = 240;
    private static final int Scale = 2;

    private final AnimationTimer timer = new SixtyFpsTimer();
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        BorderPane root = new BorderPane();
        root.setCenter(buildCanvas());
        root.setTop(buildMenu());

        primaryStage.setTitle(Title);
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();

        loadRom();
    }

    private Node buildCanvas() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(Width * Scale);
        imageView.setFitHeight(Height * Scale);
        imageView.setSmooth(false);
        return imageView;
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

    public void loadRom() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a ROM");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("iNES ROM", "*.nes")
        );
        File chosenRom = fileChooser.showOpenDialog(primaryStage);
        System.out.println("chosenRom = " + chosenRom);
    }

    private static final class SixtyFpsTimer extends AnimationTimer {
        private static final long NanosPerFrame = 1_000_000_000 / 60;

        @Override
        public void handle(long timestamp) {
        }
    }
}
