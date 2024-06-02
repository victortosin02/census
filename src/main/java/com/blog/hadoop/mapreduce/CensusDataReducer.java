package com.blog.hadoop.mapreduce;

import com.blog.hadoop.util.GridDBConnection;
import com.toshiba.mwcloud.gs.Container;
import com.toshiba.mwcloud.gs.GridStore;
import com.toshiba.mwcloud.gs.Row;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

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

        GridStore store = null;
        try {
            store = GridDBConnection.getGridStore();
            if (store == null) {
                throw new IOException("Unable to get GridStore connection");
            }

            // Define the container and row for inserting data
            String containerName = "census_data";
            Container<String, Row> container = store.getContainer(containerName);

            if (container == null) {
                // Create container if it does not exist
                container = store.putCollection(containerName, Row.class);
            }

            // Create a row for insertion
            Row row = container.createRow();
            row.setString(0, key.toString());
            row.setInteger(1, minIncome);
            row.setInteger(2, maxIncome);
            row.setInteger(3, minFamilySize);
            row.setInteger(4, maxFamilySize);

            // Insert the row
            container.put(row);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        } finally {
            if (store != null) {
                try {
                    store.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
