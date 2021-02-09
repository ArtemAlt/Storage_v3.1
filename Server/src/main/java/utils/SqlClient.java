package utils;

import java.nio.file.Paths;
import java.sql.*;
@lombok.extern.slf4j.Slf4j

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
    public synchronized static void regUser(String login, String password){

        String userPath = Configs.serverPath+ "/" +login;
        String query = String.format("insert into Users (Login, Password,User_path) VALUES ('%s','%s','%s')",login, password,userPath);
        try {
            statement.executeUpdate(query);
        } catch (SQLException e) {
           log.debug("SQL exception");
        } finally {
            if(statement!=null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.debug("SQL exit exception");
                }
            }
        }
    }


    synchronized public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
