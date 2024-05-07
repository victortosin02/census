package com.blog.hadoop.jobs;

import com.blog.hadoop.util.JobParams;
import org.apache.hadoop.mapreduce.Job;

import java.io.IOException;

public interface HadoopJob {

    boolean runJob(JobParams jobParams);

    Job createJob() throws IOException;

    void setMapperAndReducer(Job job);

    void setInputAndOutputFormatClass(Job job);

    void setInputOutputPaths(Job job, JobParams jobParams) throws IOException;
}
