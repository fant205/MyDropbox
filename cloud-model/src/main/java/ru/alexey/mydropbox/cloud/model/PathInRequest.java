package ru.alexey.mydropbox.cloud.model;

public class PathInRequest implements CloudMessage {

    private String folderName;

    public PathInRequest(String folderName){
        this.folderName = folderName;
    }


    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}
