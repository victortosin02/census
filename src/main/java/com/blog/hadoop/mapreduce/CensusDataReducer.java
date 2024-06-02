package com.blog.hadoop.mapreduce;

import com.blog.hadoop.util.GridDBConnection;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CensusDataReducer extends Reducer<Text, Text, Text, NullWritable> {
    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        int minIncome = Integer.MAX_VALUE;
        int maxIncome = Integer.MIN_VALUE;
        int minFamilySize = Integer.MAX_VALUE;
        int maxFamilySize = Integer.MIN_VALUE;
        for (Text value : values) {
            String[] parts = value.toString().split(",");
            int income = Integer.parseInt(parts[0]);
            int familySize = Integer.parseInt(parts[1]);
            minIncome = Math.min(minIncome, income);
            maxIncome = Math.max(maxIncome, income);
            minFamilySize = Math.min(minFamilySize, familySize);
            maxFamilySize = Math.max(maxFamilySize, familySize);
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = GridDBConnection.getConnection();
            if (connection == null) {
                throw new IOException("Unable to get GridDB connection");
            }

            String insertSQL = "INSERT INTO census_data (occupation, min_income, max_income, min_family_size, max_family_size) " +
                    "VALUES (?, ?, ?, ?, ?)";
            preparedStatement = connection.prepareStatement(insertSQL);
            preparedStatement.setString(1, key.toString());
            preparedStatement.setInt(2, minIncome);
            preparedStatement.setInt(3, maxIncome);
            preparedStatement.setInt(4, minFamilySize);
            preparedStatement.setInt(5, maxFamilySize);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}