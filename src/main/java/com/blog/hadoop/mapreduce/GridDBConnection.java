package com.blog.hadoop.mapreduce;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class GridDBConnection {
    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("com.toshiba.griddb.jdbc.GridDBDriver");
                connection = DriverManager.getConnection(
                        "jdbc:griddb://" + System.getProperty("griddb.url") + "/",
                        System.getProperty("griddb.username"),
                        System.getProperty("griddb.password")
                );
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}