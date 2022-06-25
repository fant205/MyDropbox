package netty.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilesServiceImpl implements FilesService {

    private Connection connection;

    @Override
    public void start() {


        try {
//            connection = DriverManager.getConnection("jdbc:postgresql://localhost/java3", "postgres", "!QAZ1qaz");
            connection = DriverManager.getConnection(
//                    "jdbc:postgres://fnrtuqrj:NLpOUejgCpyCN9POcNC7XlDtSK3h4Hw6@tyke.db.elephantsql.com/fnrtuqrj",
                    "jdbc:postgresql://tyke.db.elephantsql.com:5432/fnrtuqrj",
                    "fnrtuqrj",
                    "NLpOUejgCpyCN9POcNC7XlDtSK3h4Hw6");
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

    public List<String> findUsers() {
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT * FROM users");
            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("nickname"));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void shareFile(String fileName, String filePath, String ownerUser, String targetUser) {
        //TODO
        String preparedSql = " call shareFile2(?,?,?,?)";
        try (CallableStatement cstmt = connection.prepareCall(preparedSql)) {
            cstmt.setString(1, fileName);
            cstmt.setString(2, filePath);
            cstmt.setString(3, ownerUser);
            cstmt.setString(4, targetUser);
            cstmt.execute();
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
    public void createUser(String login, String password, String nickname) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO users (login, password, nickname) VALUES (?,?,?)")) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, nickname);
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

    public Map<String, Integer> findShareFiles(String targetUser) {
        String sql = "select file_name, file_path, u.nickname" +
                " from users" +
                " inner join files_share" +
                " on users.id = files_share.target_user_id" +
                " inner join files" +
                " on files.id = files_share.file_id" +
                " inner join users u" +
                " on u.id = files.owner_id" +
                " where users.nickname = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, targetUser);
            ps.execute();
            try (ResultSet resultSet = ps.getResultSet()) {
                Map<String, Integer> files = new HashMap<>();
                while (resultSet.next()) {
//                    String s = String.format("%s (от %s)", resultSet.getString("file_name"), resultSet.getString("nickname"));
//                    files.put(s, 0);
                    files.put(resultSet.getString("file_name"), 0);
                }
                return files;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String findFile(String file) {
        String sql = "select file_path" +
                " from files" +
                " where files.file_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, file);
            ps.execute();
            try (ResultSet resultSet = ps.getResultSet()) {
                Map<String, Integer> files = new HashMap<>();
                resultSet.next();
                return resultSet.getString("file_path");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}