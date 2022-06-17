package ru.alexey.mydropbox.cloud.app;

import javafx.scene.control.Alert;

public class Utils {

    public static void error(String title, String text) {
        error(title, null, text);
    }

    public static void error(String title, String headerText, String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
