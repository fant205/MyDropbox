package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class ShowShareMessage implements CloudMessage {

    private String targetUser;

    public ShowShareMessage(String targetUser){
        this.targetUser = targetUser;
    }

}
