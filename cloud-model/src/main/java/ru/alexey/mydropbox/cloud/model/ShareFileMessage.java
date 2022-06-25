package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class ShareFileMessage implements CloudMessage {

    private String fileName;
    private String ownerUser;
    private String targetUser;

    public ShareFileMessage(String fileName, String ownerName, String targetUser) {
        this.fileName = fileName;
        this.ownerUser = ownerName;
        this.targetUser = targetUser;
    }
}