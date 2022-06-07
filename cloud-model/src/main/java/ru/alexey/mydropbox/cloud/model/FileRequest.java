package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class FileRequest implements CloudMessage {

    private final String name;

}
