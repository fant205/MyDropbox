module ru.alexey.mydropbox.cloud.app {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.alexey.mydropbox.cloud.app to javafx.fxml;
    exports ru.alexey.mydropbox.cloud.app;
}