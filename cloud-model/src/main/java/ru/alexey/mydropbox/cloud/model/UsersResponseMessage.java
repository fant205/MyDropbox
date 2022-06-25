package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

import java.util.List;

@Data
public class UsersResponseMessage implements CloudMessage {

    private List<String> users;

    public UsersResponseMessage(List<String> users){
        this.users = users;
    }
}