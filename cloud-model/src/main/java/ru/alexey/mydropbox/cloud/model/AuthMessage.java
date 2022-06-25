package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class AuthMessage implements CloudMessage {

    private String login;
    private String password;


    public AuthMessage(String login, String pass) {
        this.login = login;
        this.password = pass;
    }
}
