package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class FileOnShareRequest implements CloudMessage {
    private String file;

    public FileOnShareRequest(String file) {
        this.file = file;
    }
}
