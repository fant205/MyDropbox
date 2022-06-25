package netty.service;

import java.util.List;
import java.util.Map;

public interface FilesService {
    void start();

    String getNickNameByLoginAndPassword(String login, String password);

    public void changeNickname(String oldNickName, String newNickname);

    public void createUser(String login, String password, String nickname);

    void end();

    List<String> findUsers();

    public void shareFile(String fileName, String filePath, String ownerUser, String targetUser);

    public Map<String, Integer> findShareFiles(String targetUser);

    public String findFile(String file);
}
