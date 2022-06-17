package netty.service;

import java.sql.*;

public class DataBaseAuthServiceImpl implements AuthService {

    private Connection connection;

    @Override
    public void start() {
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost/java3", "postgres", "!QAZ1qaz");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNickNameByLoginAndPassword(String login, String password) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT nickname FROM users WHERE login = ? AND password = ?")) {
            ps.setString(1, login);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("nickname");
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void changeNickname(String oldNickName, String newNickname) {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?")) {
            ps.setString(1, newNickname);
            ps.setString(2, oldNickName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void end() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
