package com.blog.hadoop.driver;

import com.blog.hadoop.jobs.CensusDataAnalyzerJob;

public class AppMain {
    public static void main(String[] args) {
        // Check if the correct number of arguments is provided
        if (args.length != 1) {
            // Print usage instructions if the arguments are incorrect
            System.out.println("Usage: hadoop jar target/census-data-analysis-1.0.jar /path/to/input.txt");
            System.exit(1); // Exit the program with a status code of 1 (indicating an error)
        }

        // Create an instance of CensusDataAnalyzerJob and run the job with the provided arguments
        boolean success = new CensusDataAnalyzerJob().runJob(args);
        // If the job fails, exit the program with a status code of 1
        if (!success) {
            System.exit(1);
        }
    }
}
