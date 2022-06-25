package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class ErrorMessage implements CloudMessage {

    private String msg;

    public ErrorMessage(String msg) {
        this.msg = msg;
    }
}
