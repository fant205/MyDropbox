module ru.alexey.mydropbox.cloudapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens ru.alexey.mydropbox.cloudapp to javafx.fxml;
    exports ru.alexey.mydropbox.cloudapp;
}