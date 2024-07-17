# Analyzing Census Data with Java, JDBC, GridDB, and Hadoop

## Introduction
Census data analytics systems play a pivotal role in processing, analyzing, and deriving insights from demographic information collected through regular population censuses. This blog post delves into the development of a comprehensive census analytics system, leveraging Java, Hadoop, JDBC, and GridDB technologies. The primary objective is to analyze and calculate minimum and maximum income along with family size for each occupation, storing the results efficiently in a GridDB database.

## How to Follow Along
- **Clone the repository:**
To explore the implementation details, you can access the source code from our repository:

`$ git clone https://github.com/victortosin02/census`

`$ cd census`

- **Build the project using Maven:**

`$ mvn package`

- **Run the Hadoop job:**

`$ hadoop jar target/census-data-analysis-1.0.jar /path/to/input.txt`

Replace input with the path to your input data.

## Technologies Used:
The project utilizes a stack of technologies for efficient census data analysis:
- Java: Primary programming language.
- Hadoop: Distributed processing framework.
- JDBC: Database connectivity.
- GridDB: Storage and management of processed data.

## Benefits of Census Analytics System
The census analytics system offers several advantages, including:
- Enhanced data processing efficiency with Hadoop's distributed computing framework.
- Scalable data management using GridDB's storage solution.
- Flexible data analysis capabilities facilitated by JDBC.
- Extraction of valuable insights into census trends using Java.

## What are the operational processes and workflow of this project?
1. **Data Ingestion:** Reading census data from a txt file.
2. **Data Processing:** Employing Hadoop to calculate minimum and maximum income and family size for each occupation.
3. **Data Storage:** Storing processed data in GridDB database using JDBC.
4. **Data Analysis:** Retrieving and analyzing data from GridDB to gain insights into census trends.

## What We’re Building
In this guide, we will embark on a journey to create a census analytics system capable of ingesting, processing and analyzing census data to generate valuable insights that will help in proper decision making. This system aim to empower stakeholders to properly plan and allocate resources efficiently.

## Prerequisites
What you need to install:
- Java version: 17 and above
- Maven version: 3.*
- Hadoop version: 3.2.4 (Apache distribution)
- GridDB version 5.5.0

## Project Overview
The Census Analytics System is a robust platform designed to process, analyze, and generate insights from census data. Leveraging Java, Hadoop, JDBC, and GridDB, the system offers scalable and efficient solutions for census data management and analysis.

By leveraging Java, Hadoop, JDBC, and GridDB, we are able to derive valuable insights from census data, enabling data-driven decision-making and policy formulation. With its scalable architecture and powerful analytical capabilities, the system serves as a valuable tool for researchers, policymakers, and businesses seeking to understand and address demographic challenges.

## Setting up the Project
- Add necessary dependencies to the `pom.xml` file.
```
<dependencies>
    <dependency>
        <groupId>org.apache.hadoop</groupId>
        <artifactId>hadoop-client</artifactId>
        <version>3.2.4</version>
    </dependency>
    <dependency>
        <groupId>com.toshiba.griddb</groupId>
        <artifactId>gridstore</artifactId>
        <version>5.5.0</version>
    </dependency>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.12</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```
## Setting Up Hadoop and Configuration Files
- Install Hadoop: Follow the official Apache Hadoop installation guide for your operating system. Ensure Java is installed and configured correctly as Hadoop is Java-based.
- Configure Hadoop: Create a resources folder in your project directory to store Hadoop configuration files.
- Add Configuration Files:
## yarn-site.xml
```
<configuration>
    <!-- ResourceManager address -->
    <property>
        <name>yarn.resourcemanager.address</name>
        <value>localhost:8032</value>
    </property>

    <!-- Scheduler address -->
    <property>
        <name>yarn.resourcemanager.scheduler.address</name>
        <value>localhost:8030</value>
    </property>

    <!-- ResourceManager WebApp address -->
    <property>
        <name>yarn.resourcemanager.webapp.address</name>
        <value>localhost:8088</value>
    </property>

    <!-- NodeManager auxiliary services -->
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>

    <!-- Container memory settings -->
    <property>
        <name>yarn.nodemanager.resource.memory-mb</name>
        <value>2048</value>
    </property>
</configuration>
```
## core-site.xml
```
<configuration>
    <!-- Define default filesystem and address -->
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://localhost:9000</value>
    </property>

    <!-- Hadoop home directory -->
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/app/hadoop/tmp</value>
        <description>A base for other temporary directories.</description>
    </property>

    <!-- I/O File Buffer Size -->
    <property>
        <name>io.file.buffer.size</name>
        <value>131072</value>
    </property>
</configuration>
```
## hdfs-file.xml
```
<configuration>
    <!-- NameNode specific properties -->
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>file:///app/hadoop/dfs/name</value>
    </property>

    <!-- DataNode specific properties -->
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>file:///app/hadoop/dfs/data</value>
    </property>

    <!-- Block replication factor -->
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>

    <!-- Permissions and quota -->
    <property>
        <name>dfs.permissions</name>
        <value>false</value>
    </property>
</configuration>
```
## mapred-site.xml
```
<configuration>
    <!-- JobTracker settings -->
    <property>
        <name>mapreduce.jobtracker.address</name>
        <value>localhost:54311</value>
    </property>

    <!-- Framework name -->
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>

    <!-- Specify MapReduce job history directory -->
    <property>
        <name>mapreduce.jobhistory.done-dir</name>
        <value>/mr-history/done</value>
    </property>
</configuration>
```

## Data Ingestion
As the first workflow in our project architecture, we will leverage on a mapper class in a Hadoop MapReduce job, specifically tailored for the purpose of data ingestion. This mapper class parses each line of input data, extracts relevant fields (occupation, income, and family size), and emits key-value pairs where the occupation serves as the key and income along with family size is the value. This processed data will be further aggregated and analyzed in subsequent stages of the MapReduce job. Below is the code that handles data ingestion into our system.

```
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class CensusDataMapper extends Mapper<LongWritable, Text, Text, Text> {
    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        // Ignore header line
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
```
## Data Analytics
After data ingestion using the CensusDataMapper, it is important to align the analysis with the deliverables we seek to achieve. Bearing this in mind, we create another class which represents the reducer class in our Hadoop MapReduce job, specifically dedicated to performing data analytics in ths system. This reducer class aggregates the income and family size data associated with each occupation group and calculates the minimum and maximum values for each occpuation. It then emits this aggregated information for further analysis or storage in subsequent stages of the MapReduce job. Below is the code that further corroborates our explanation from a code context:
```
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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

        Connection connection = GridDBConnection.getConnection();
        try (Statement statement = connection.createStatement()) {
            String insertSQL = String.format(
                "INSERT INTO census_data (occupation, min_income, max_income, min_family_size, max_family_size) " +
                "VALUES ('%s', %d, %d, %d, %d)", key.toString(), minIncome, maxIncome, minFamilySize, maxFamilySize);
            statement.execute(insertSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

## Data Processing
After the aggregation of the income and family size data associated with each occupation group coupled with the minimum and maximum values for the occupations groups, we then define a class CensusDataAnalyzerJob responsible for setting up and running a Hadoop MapReduce job to analyze census data. Below are the code snippets that handles running of the Hadoop job for processing the analyzed data:
```
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
```
Let's break down what each part of the code is doing:
## Class Definition
The CensusDataAnalyzerJob class contains a single method, runJob, which is responsible for configuring and running the Hadoop job.

## Method: runJob()
## Parameters
- **String[] args:** This array contains the command-line arguments passed to the job, typically including the input path to the data.

## Configuration Initialization
`Configuration conf = new Configuration();`
- Creates a new Configuration object which holds the configuration settings for the Hadoop job.

## Job Instance Creation
`Job job = Job.getInstance(conf, "Census Data Analysis");`
- Creates a new Job instance with the given configuration and sets the job name to "Census Data Analysis".

## Set Jar Class
`job.setJarByClass(AppMain.class);`
- Specifies the class that contains the main method (AppMain) to be used for finding the job's JAR file. This helps Hadoop locate the JAR file containing the job code when running on the cluster.

## Set Mapper and Reducer Classes
```job.setMapperClass(CensusDataMapper.class);
job.setReducerClass(CensusDataReducer.class);
```
- Sets the mapper class to CensusDataMapper and the reducer class to CensusDataReducer.

**Set Output Key and Value Classes**
```job.setOutputKeyClass(Text.class);
job.setOutputValueClass(Text.class);
```
- Specifies the output key and value types for the job. Both are set to Text class.

## Input and Output Configuration
## Set Input Path
`FileInputFormat.addInputPath(job, new Path(args[0]));`
- Sets the input path for the job to the path specified in the first command-line argument (args[0]).

## Set Output Path
`job.setOutputFormatClass(NullOutputFormat.class);`
- Specifies that the job does not produce any output files. The NullOutputFormat class is used when the job's results do not need to be written to HDFS (e.g., when results are written directly to a database).

## Job Execution
## Run the Job
`return job.waitForCompletion(true);`
- Submits the job to the cluster and waits for it to complete. Returns true if the job completes successfully, otherwise false.

## Exception Handling
``` } catch (Exception e) {
    e.printStackTrace();
    return false;
}
```
- Catches any exceptions that occur during the job setup or execution, prints the stack trace for debugging, and returns false indicating the job failed.

## Summary
The CensusDataAnalyzerJob class configures a Hadoop MapReduce job to analyze census data by:
- Setting up the mapper and reducer classes.
- Specifying input paths and output formats.
- Submitting the job to the Hadoop cluster and waiting for it to complete.
The job reads input data from a specified path, processes it using the mapper and reducer, and since the output format is NullOutputFormat, it does not write the results to HDFS but likely writes the processed results to a database as defined in the CensusDataReducer class.

## Running the Hadoop Job
To facilitate the running of the Hadoop job for analyzing census data, we need to create an AppMain class which is designed to initiate a Hadoop MapReduce job for analyzing census data provided in a txt file. It ensures the correct usage of the command-line arguments, and if successful, it runs the job. Below is the code for AppMain class:
```
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
```

Now let's break down what each part of the code is doing:
## Argument Checking:
```if (args.length != 1) {
    System.out.println("Usage: hadoop jar target/census-data-analysis-1.0.jar /path/to/input.txt");
    System.exit(1);
}
```
- The program expects exactly one command-line argument, which should be the path to the input txt file.
- If the number of arguments is not equal to one, it prints the correct usage and exits with a status code of 1, indicating an error.

## Running the Job
`boolean success = new CensusDataAnalyzerJob().runJob(args);`

- It creates an instance of the CensusDataAnalyzerJob class and calls its runJob method, passing the command-line arguments to it.
- The runJob method is responsible for setting up and executing the Hadoop MapReduce job.

## Handling Job Success/Failure
```if (!success) {
    System.exit(1);
}
```
- The runJob method returns a boolean value indicating whether the job was successful or not.
- If the job did not succeed (success is false), the program exits with a status code of 1.

To run this program, you would use the Hadoop command line interface and specify the path to the input txt file

`$ hadoop jar target/census-data-analysis-1.0.jar /path/to/input.txt`


## Data Storage
## Setting up JDBC for GridDB
- Create griddb.properties file with GridDB configuration
```
griddb.username=admin
griddb.password=admin
griddb.notificationMember=127.0.0.1:10001
griddb.database=census_data
```
- Create GridDBConnection class to establish JDBC connection
Here we define a class named GridDBConnection, which is responsible for establishing a JDBC connection to a GridDB database. This GridDBConnection class provides a reusable method (getConnection()) for obtaining a JDBC connection to a GridDB database, ensuring efficient management of database connections within Java applications.
```
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class GridDBConnection {
    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
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

                Class.forName("com.toshiba.griddb.jdbc.GridDBDriver");
                String url = "jdbc:griddb:///" + properties.getProperty("griddb.database") + "?notificationMember=" + properties.getProperty("griddb.notificationMember");
                connection = DriverManager.getConnection(
                        url,
                        properties.getProperty("griddb.username"),
                        properties.getProperty("griddb.password")
                );
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
```

- Modify CensusDataReducer class to store processed data in GridDB
As a final step, we need to modify the CensusDataReducer class to store processed data in a GridDB database. This modified CensusDataReducer class integrates with GridDB to store processed census data. It establishes a JDBC connection to the GridDB database, and for each occupation group, it executes an SQL INSERT statement to store the calculated minimum and maximum income along with minimum and maximum family size in the census_data table.

```
public class CensusDataReducer extends Reducer<Text, Text, Text, Text> {
    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        // ...
        Connection connection = GridDBConnection.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO census_data (occupation, min_income, max_income, min_family_size, max_family_size) VALUES ('" + key.toString() + "', " + minIncome + ", " + maxIncome + ", " + minFamilySize + ", " + maxFamilySize + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

Now, the processed data will be stored in the census_data table in the GridDB database.

## Conclusion:
Census Data Analytics using Java, Hadoop, JDBC, and GridDB is a powerful project that demonstrates the strengths of each technology in handling large and complex datasets. By leveraging these technologies, organizations can extract valuable insights from census data, informing decision-making and driving business success.
