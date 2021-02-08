package utils;

import java.sql.*;

public class SqlClient {
    private static Connection connection;
    private static Statement statement;


    public synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:Server/src/main/resources/DBStorage.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized public static String getUserPath(String login, String password) {
        String query = String.format("select User_path from Users where Login = '%s' and Password = '%s'",
                login, password);

        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next())
                return set.getString(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;

    }


    synchronized public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
