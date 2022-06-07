module ru.alexey.mydropbox.cloud.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.netty.codec;
    requires ru.alexey.mydropbox.cloud.model;
    requires lombok;


    opens ru.alexey.mydropbox.cloud.app to javafx.fxml;
    exports ru.alexey.mydropbox.cloud.app;
}