package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

@Data
public class SuccessAuthorization implements CloudMessage{

    private String nickname;

    public SuccessAuthorization(String nickname){
        this.nickname = nickname;
    }
}
