package org.xokyopo.server.dao;

import java.sql.*;

public class DataBaseManager {
    private static Connection connection;
    private static Statement statement;

    public static void connection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:users.sqlite");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void disconnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public synchronized static String getUserPassword(String name) {
        String sql = String.format("SELECT pass FROM users WHERE name = '%s'", name);
        try {
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                return rs.getString("pass");
            }
        } catch (SQLException e) {
            //ошибка запроса?
            e.printStackTrace();
        }
        return null;
    }

    public synchronized static void addClient(String login, String password) {
        String sql = String.format("insert into users (name, pass) values('%s','%s')", login, password);
        try {
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
