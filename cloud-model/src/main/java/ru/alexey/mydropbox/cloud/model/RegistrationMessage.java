package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class RegistrationMessage implements CloudMessage {

    private String login;
    private String password;
    private String nickname;

    public RegistrationMessage(String login, String password, String nickname){
        this.login = login;
        this.password = password;
        this.nickname = nickname;
    }
}
