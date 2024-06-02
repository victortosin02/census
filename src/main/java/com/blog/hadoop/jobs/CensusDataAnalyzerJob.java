package com.blog.hadoop.jobs;

import com.blog.hadoop.driver.AppMain;
import com.blog.hadoop.mapreduce.CensusDataMapper;
import com.blog.hadoop.mapreduce.CensusDataReducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;

public class CensusDataAnalyzerJob {
    public boolean runJob(String[] args) {
        try {
            Configuration conf = new Configuration();
            Job job = Job.getInstance(conf, "Census Data Analysis");
            job.setJarByClass(AppMain.class);
            job.setMapperClass(CensusDataMapper.class);
            job.setReducerClass(CensusDataReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(Text.class);

            FileInputFormat.addInputPath(job, new Path(args[0]));
            job.setOutputFormatClass(NullOutputFormat.class);

            return job.waitForCompletion(true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
