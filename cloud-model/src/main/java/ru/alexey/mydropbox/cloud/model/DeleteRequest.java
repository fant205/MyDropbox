package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Data
public class DeleteRequest implements CloudMessage {

    private final String name;

    public DeleteRequest(String name) throws IOException {
        this.name = name;
    }

}
