package com.blog.hadoop.driver;

import com.blog.hadoop.jobs.CensusDataAnalyzerJob;
import com.blog.hadoop.util.JobParams;

public class AppMain {
    public static void main(String[] args) {
        JobParams jobParams = new JobParams("input", "output");
        if (args.length == 2) {
            System.out.println("Using custom job params.");
            jobParams.setInputPath(args[0]);
            jobParams.setOutputPath(args[1]);
        } else {
            System.out.println("Usage: $hadoop jar target/census-data-analysis-1.0.jar input output");
            System.exit(1);
        }
        boolean success = new CensusDataAnalyzerJob().runJob(jobParams);
        if (!success) {
            System.exit(1);
        }
    }
}