package dev.jackraidenph.logicevaluator;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;

public class Main extends javafx.application.Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 640, 480);
        stage.setResizable(false);
        stage.setTitle("Logic Evaluator");
        stage.setScene(scene);
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            Node node = event.getPickResult().getIntersectedNode();
            if (node.getParent() instanceof Label text) {
                ClipboardContent content = new ClipboardContent();
                content.putString(text.getText().split(":")[1].trim());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}