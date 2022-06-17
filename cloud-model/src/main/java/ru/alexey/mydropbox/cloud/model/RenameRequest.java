package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

import java.io.IOException;

@Data
public class RenameRequest implements CloudMessage{
    private final String oldName;
    private final String newName;

    public RenameRequest(String oldName, String newName) throws IOException {
        this.oldName = oldName;
        this.newName = newName;
    }
}
