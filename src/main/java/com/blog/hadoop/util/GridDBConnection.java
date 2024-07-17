package com.blog.hadoop.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class GridDBConnection {
    private static Connection connection;

    public static Connection getConnection() throws IOException, SQLException {
        if (connection == null) {
            Properties properties = new Properties();
            try (InputStream input = GridDBConnection.class.getClassLoader().getResourceAsStream("griddb.properties")) {
                if (input == null) {
                    System.out.println("Sorry, unable to find griddb.properties");
                    return null;
                }
                properties.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }

            String url = "jdbc:gs://" + properties.getProperty("griddb.notificationMember") + "/" + properties.getProperty("griddb.clusterName");
            String user = properties.getProperty("griddb.user");
            String password = properties.getProperty("griddb.password");

            try {
                connection = DriverManager.getConnection(url, user, password);
            } catch (SQLException e) {
                e.printStackTrace();
                throw new IOException("Failed to get JDBC connection", e);
            }
        }
        return connection;
    }
}