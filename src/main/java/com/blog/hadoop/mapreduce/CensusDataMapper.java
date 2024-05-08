package com.blog.hadoop.mapreduce;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class CensusDataMapper extends Mapper<LongWritable, Text, Text, Text> {
    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        // Ignore header line starting with @
        if (line.startsWith("@")) {
            return;
        }
        String[] parts = line.split(",");
        if (parts.length != 4) {
            // Skip malformed input
            return;
        }
        String occupation = parts[2].trim();
        String income = parts[3].trim();
        String familySize = parts[1].trim();
        // Emit occupation as key and income,familySize as value
        context.write(new Text(occupation), new Text(income + "," + familySize));
    }
}