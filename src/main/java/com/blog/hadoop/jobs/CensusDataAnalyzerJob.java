package com.blog.hadoop.jobs;

import com.blog.hadoop.util.JobParams;
import com.blog.hadoop.driver.AppMain;
import com.blog.hadoop.mapreduce.CensusDataMapper;
import com.blog.hadoop.mapreduce.CensusDataReducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

public class CensusDataAnalyzerJob implements HadoopJob {
    @Override
    public boolean runJob(JobParams jobParams) {
        try {
            // Create job
            Job job = createJob();
            // Set Mapper and Reducer
            setMapperAndReducer(job);
            // Set input/output format classes
            setInputAndOutputFormatClass(job);
            // Set input/output paths
            setInputOutputPaths(job, jobParams);
            job.setJarByClass(AppMain.class);
            job.setJobName("Census Data Analysis: Min-Max");
            job.setWorkingDirectory(new Path(Path.CUR_DIR));
            // Submit the job to the cluster and wait for it to finish
            return job.waitForCompletion(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
    // ...

    @Override
    public com.blog.hadoop.jobs.Job createJob() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createJob'");
    }

    @Override
    public void setMapperAndReducer(com.blog.hadoop.jobs.Job job) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setMapperAndReducer'");
    }

    @Override
    public void setInputAndOutputFormatClass(com.blog.hadoop.jobs.Job job) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInputAndOutputFormatClass'");
    }

    @Override
    public void setInputOutputPaths(com.blog.hadoop.jobs.Job job, JobParams jobParams) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setInputOutputPaths'");
    }
}
