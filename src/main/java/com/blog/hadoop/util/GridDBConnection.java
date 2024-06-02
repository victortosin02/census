package com.blog.hadoop.util;

import com.toshiba.mwcloud.gs.GridStore;
import com.toshiba.mwcloud.gs.GridStoreFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GridDBConnection {
    private static GridStore store;

    public static GridStore getGridStore() throws IOException {
        if (store == null) {
            try {
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

                Properties props = new Properties();
                props.setProperty("notificationMember", properties.getProperty("griddb.notificationMember"));
                props.setProperty("clusterName", properties.getProperty("griddb.clusterName"));
                props.setProperty("user", properties.getProperty("griddb.user"));
                props.setProperty("password", properties.getProperty("griddb.password"));

                store = GridStoreFactory.getInstance().getGridStore(props);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Failed to get GridStore connection", e);
            }
        }
        return store;
    }
}