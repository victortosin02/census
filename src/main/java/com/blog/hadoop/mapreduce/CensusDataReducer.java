package com.blog.hadoop.mapreduce;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class CensusDataReducer extends Reducer<Text, Text, Text, Text> {
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
        context.write(key, new Text(minIncome + "," + maxIncome + "," + minFamilySize + "," + maxFamilySize));
    }
}
