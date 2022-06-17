package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class ErrorAuthorization implements CloudMessage {

    private String msg;

    public ErrorAuthorization(String msg) {
        this.msg = msg;
    }
}
