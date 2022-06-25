package ru.alexey.mydropbox.cloud.app;

import javafx.scene.control.Alert;

public class Utils {

    public static void error(String title, String text) {
        error(title, null, text);
    }

    public static void error(String title, String headerText, String text) {
        msg(title, headerText, text, Alert.AlertType.ERROR);
    }

    public static void info(String title, String headerText, String text) {
        msg(title, headerText, text, Alert.AlertType.INFORMATION);
    }

    public static void msg(String title, String headerText, String text, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
