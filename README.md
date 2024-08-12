# Census Data Analytics Using Griddb, Docker and NodeJS
## Introduction
In this project, we will be concentrating on census analytics system designed to ingest data from a source system, analyze and generate insights from census data, and finally store the processed data in a target syatem. Census data in this case refers to the demographic information retrieved from a population at regular intervals, typically conducted by governments to gather information about the population's characteristics, such as age, gender, race, income, education level, household composition, and more. The census analytics system has proved efficient where census officials can upload census data from household enumeration and insights are generated after the processing operations on this data. To narrow this down in terms of deliverables, this project seeks to analyze and calculate the minimum and maximum income and family size for each occupation and store the results in a GridDB database leveraging on stacks of technologies such as Node JS, JDBC, Docker and GridDB.

## Methodology
The purpose of this project is to analyze sample data of households from an enumeration exercise from a source system, analyze the ingested data and load analyzed data to a database. To accomplish this, we will be leveraging Docker because it makes it easy to spin up all the required services.

## Technologies Used:
The following stack of technologies were leveraged on to efficiently analyze the census data: Java for programming

- Javasccript for programming
- JDBC for database connectivity
- GridDB for storing and managing data
- Docker For Containerization and Application Portability

## Prerequisites
What you need to install:

- NodeJS
- Docker Desktop
- Griddb will be downloaded and installed in a Dockerfile in consquent sections.

##  How to Follow Along
If you plan to code along yourself while you read this article, you can grab the source code from the repo below:

`git clone https://github.com/victortosin02/census.git`

Once you have cloned the repo or downloaded the project folders and files, you need to build and start the griddb server and later get into the data-analyzer.js script that pushes rows of data into the provisioned Griddb database.

To spin up the application, simply run:

`docker-compose up --build`

This starts the griddb and census-analyzer containers in the docker.yaml file:

## Server Provisioning and Configuration
To efficiently handle the scalability and volume of census data, we leverage Docker for containerization and GridDB for data management. The following steps outline the provisioning and configuration of the GridDB server.

### Step 1: Create a Dockerfile
We start by creating a Dockerfile to define the environment and installation steps for GridDB.

```
FROM ubuntu:22.04

# You can download griddb V5.0.0 directly at https://github.com/griddb/griddb/releases/tag/v5.0.0
ENV GRIDDB_VERSION=5.6.0
ENV GS_HOME=/var/lib/gridstore
# Need declare $GS_LOG to start GridDB server
ENV GS_LOG=/var/lib/gridstore/log
ENV PORT=10001
ENV DEBIAN_FRONTEND=noninteractive

# Install griddb server
RUN set -eux \
    && apt-get update \
    # Install dependencies for griddb
    && apt-get install -y systemd dpkg python3 wget jq default-jre --no-install-recommends \
    && apt-get clean all \
    # Download package griddb server
    && wget -q https://github.com/griddb/griddb/releases/download/v${GRIDDB_VERSION}/griddb_${GRIDDB_VERSION}_amd64.deb --no-check-certificate \
    # Install package griddb server
    && dpkg -i griddb_${GRIDDB_VERSION}_amd64.deb \
    # Remove package
    && rm griddb_${GRIDDB_VERSION}_amd64.deb \
    # Delete the apt-get lists after installing something
    && rm -rf /var/lib/apt/lists/*

# Install GridDB c_client
RUN wget --no-check-certificate https://github.com/griddb/c_client/releases/download/v${GRIDDB_VERSION}/griddb-c-client_${GRIDDB_VERSION}_amd64.deb
RUN dpkg -i griddb-c-client_${GRIDDB_VERSION}_amd64.deb

RUN wget --no-check-certificate https://github.com/griddb/cli/releases/download/v5.3.1/griddb-ce-cli_5.3.1_amd64.deb
RUN dpkg -i griddb-ce-cli_5.3.1_amd64.deb

ADD start-griddb.sh /
ADD .gsshrc /root
RUN chmod +x start-griddb.sh
USER gsadm

ENTRYPOINT ["/bin/bash", "/start-griddb.sh"]
EXPOSE $PORT
CMD ["griddb"]
```
### Step 2: Configure GridDB
We will be creating a .gsshrc file which is crucial for setting up the GridDB cluster in FIXED_LIST mode.
```
setnode node0 127.0.0.1 10040 22
setcluster cluster0 myCluster FIXED_LIST 127.0.0.1:10001 $node0
setclustersql cluster0 myCluster FIXED_LIST 127.0.0.1:20001
setuser admin admin
connect $cluster0
```
### Step 3: Create the Startup Script
The start-griddb.sh script handles the initialization and configuration of GridDB, including setting up the cluster in FIXED_LIST mode and applying the necessary configurations.
```
#!/bin/bash

if [ "${1:0:1}" = '-' ]; then
    set -- griddb "$@"
fi

# Save configuration to file
save_config() {
    echo "GRIDDB_CLUSTER_NAME=\"$GRIDDB_CLUSTER_NAME\"" >> /var/lib/gridstore/conf/gridstore.conf
    echo "GRIDDB_USERNAME=\"$GRIDDB_USERNAME\""         >> /var/lib/gridstore/conf/gridstore.conf
    echo "GRIDDB_PASSWORD=\"$GRIDDB_PASSWORD\""         >> /var/lib/gridstore/conf/gridstore.conf
}

# Get machine IP address
get_ipadress() {
    ip_address=$(hostname -I | awk '{print $1}')
}

# Configure FIXED_LIST mode
fixlist_config() {
    jq 'del(.cluster.notificationAddress) | del(.cluster.notificationPort)' /var/lib/gridstore/conf/gs_cluster.json | tee tmp.json > /dev/null
    jq '.cluster |= .+ {"notificationMember": [{"cluster":{"address", "port":10010}, "sync":{"address","port":10020}, "system":{"address", "port":10040}, "transaction":{"address", "port":10001}, "sql":{"address", "port":20001}}]}' tmp.json | tee tmp_gs_cluster.json >/dev/null
    mv tmp_gs_cluster.json /var/lib/gridstore/conf/gs_cluster.json
    rm tmp.json
    jq --arg ip_address "$ip_address"  '. + { serviceAddress: $ip_address}'  /var/lib/gridstore/conf/gs_node.json  >  tmp_gs_node.json
    mv tmp_gs_node.json /var/lib/gridstore/conf/gs_node.json
    sed -i -e s/\"address\":\ null/\"address\":\"$ip_address\"/g \/var/lib/gridstore/conf/gs_cluster.json
}

# Initialize GridDB system
if [ "${1}" = 'griddb' ]; then
    isSystemInitialized=0
    if [ "$(ls -A /var/lib/gridstore/data)" ]; then
        isSystemInitialized=1
    fi

    if [ $isSystemInitialized = 0 ]; then
        export GRIDDB_CLUSTER_NAME=${GRIDDB_CLUSTER_NAME:-"dockerGridDB"}
        export GRIDDB_USERNAME=${GRIDDB_USERNAME:-"admin"}
        export GRIDDB_PASSWORD=${GRIDDB_PASSWORD:-"admin"}

        cp /usr/griddb-${GRIDDB_VERSION}/conf_multicast/* /var/lib/gridstore/conf/.
        gs_passwd $GRIDDB_USERNAME -p $GRIDDB_PASSWORD
        sed -i -e s/\"clusterName\":\"\"/\"clusterName\":\"$GRIDDB_CLUSTER_NAME\"/g \/var/lib/gridstore/conf/gs_cluster.json

        if [ ! -z $NOTIFICATION_ADDRESS ]; then
            sed -i -e s/\"notificationAddress\":\"239.0.0.1\"/\"notificationAddress\":\"$NOTIFICATION_ADDRESS\"/g \/var/lib/gridstore/conf/gs_cluster.json
        fi

        if [ ! -z $NOTIFICATION_PORT ]; then
            sed -i -e s/\"notificationPort\":31999/\"notificationPort\":$NOTIFICATION_PORT/g \/var/lib/gridstore/conf/gs_cluster.json
        fi

        if [ ! -z $NOTIFICATION_MEMBER ]; then
            if [ $NOTIFICATION_MEMBER != 1 ]; then
                echo "$NOTIFICATION_MEMBER invalid. Fixed list GridDB CE mode support one member, please check again !"
                exit 1
            fi
            checkFixList=$(cat /var/lib/gridstore/conf/gs_cluster.json | grep notificationMember)
            if [ ! -z checkFixList ]; then
                get_ipadress
                fixlist_config
            fi
        fi

        if [ ! -z $NOTIFICATION_PROVIDER ]; then
            echo "Provider mode haven't support"
            exit 1
        fi

        save_config
    fi

    . /var/lib/gridstore/conf/gridstore.conf
    cd /var/lib/gridstore
    sleep 5 && gs_joincluster -u $GRIDDB_USERNAME/$GRIDDB_PASSWORD -c $GRIDDB_CLUSTER_NAME -w &
    gsserver --conf ./conf
fi
exec "$@"
```

## Data Ingestion, Processing and Loading
From the implementation end after provisioning and configuring the server environment, we will create a folder called census-analysis which comprises an input.csv file that contains census data that will be ingested and analyzed. Also, we will create a node.js script called data-analyzer.js that ingests data from the csv file, analyzes and calculates the minimum and maximum income and family size for each occupation and store the results in the GridDB database. 

Below is the NodeJS script responsible for ingesting, analyzing and loading the data to the griddb database:

```
const fs = require('fs');
const csv = require('csv-parser');
const griddb = require('griddb-node-api');
const process = require('process');

// Read CSV file and process data
function readCSV(filePath) {
    return new Promise((resolve, reject) => {
        const results = [];
        fs.createReadStream(filePath)
            .pipe(csv())
            .on('data', (data) => results.push(data))
            .on('end', () => resolve(results))
            .on('error', (error) => reject(error));
    });
}

// Process data to calculate min and max
function processData(data) {
    const occupationStats = {};

    data.forEach(row => {
        const occupation = row.occupation;
        const familySize = parseInt(row.family_size);
        const income = parseInt(row.income);

        if (!occupationStats[occupation]) {
            occupationStats[occupation] = {
                minIncome: income,
                maxIncome: income,
                minFamilySize: familySize,
                maxFamilySize: familySize
            };
        } else {
            occupationStats[occupation].minIncome = Math.min(occupationStats[occupation].minIncome, income);
            occupationStats[occupation].maxIncome = Math.max(occupationStats[occupation].maxIncome, income);
            occupationStats[occupation].minFamilySize = Math.min(occupationStats[occupation].minFamilySize, familySize);
            occupationStats[occupation].maxFamilySize = Math.max(occupationStats[occupation].maxFamilySize, familySize);
        }
    });

    return occupationStats;
}

// Insert data into GridDB
async function insertIntoGridDB(data) {
    const factory = griddb.StoreFactory.getInstance();
    const store = factory.getStore({
        "notificationMember": process.argv[2], // Using the fixed list mode notificationMember
        "clusterName": "myCluster",
        "username": "admin",
        "password": "admin"
    });

    const containerName = 'occupation_stats';
    const containerInfo = new griddb.ContainerInfo({
        name: containerName,
        columnInfoList: [
            ["occupation", griddb.Type.STRING],
            ["minIncome", griddb.Type.LONG],
            ["maxIncome", griddb.Type.LONG],
            ["minFamilySize", griddb.Type.LONG],
            ["maxFamilySize", griddb.Type.LONG]
        ],
        type: griddb.ContainerType.COLLECTION,
        rowKey: true
    });

    const container = await store.putContainer(containerInfo);
    await container.createIndex({
        'columnName': 'occupation',
        'indexType': griddb.IndexType.DEFAULT
    });

    const rows = [];
    for (const [occupation, stats] of Object.entries(data)) {
        rows.push([
            occupation,
            stats.minIncome,
            stats.maxIncome,
            stats.minFamilySize,
            stats.maxFamilySize
        ]);
    }

    await container.multiPut(rows);
}

// Main function
async function main() {
    try {
        const csvFilePath = 'input.csv';
        const data = await readCSV(csvFilePath);
        const processedData = processData(data);
        await insertIntoGridDB(processedData);
        console.log('Data successfully inserted into GridDB.');
    } catch (error) {
        console.error('Error:', error);
    }
}

main();
```

## Dockerfile for Census Analyzer
To ensure there is seamless implementation of files in the census-analyzer, it is crucial to properly configure the Dockerfile. This Dockerfile is responsible for setting up the Node.js environment, installing necessary dependencies, and preparing the application to interact with the GridDB C Client. Below is a detailed breakdown of the Dockerfile content and its purpose. The following Dockerfile sets up a Node.js environment and installs the GridDB C Client, ensuring that the census-analyzer can effectively communicate with the GridDB server.

```
FROM node:18

# Make c_client
WORKDIR /
RUN wget --no-check-certificate https://github.com/griddb/c_client/releases/download/v5.6.0/griddb-c-client_5.6.0_amd64.deb
RUN dpkg -i griddb-c-client_5.6.0_amd64.deb


WORKDIR /app
COPY package.json /app/package.json
COPY package-lock.json /app/package-lock.json
COPY data-analyzer.js /app/data-analyzer.js

RUN npm i

ENTRYPOINT ["node", "data-analyzer.js"]
```

## Spinning All The Services Using Docker Compose Configuration
To spin up the GridDB server along with the data analyzer service, we use a docker-compose.yaml file which is responsible for spinning up the Griddb server based on the Dockerfile configurations in both griddb-server and census-analyzer folders. The docker-compose confiuration file for spinning all thses services is provided below:

```
services:

  griddb-server3:
    build:
      context: griddb-server
      dockerfile: Dockerfile
    container_name: griddb-server3
    user: root
    expose:
      - '10001'
    environment:
      NOTIFICATION_MEMBER: 1
      GRIDDB_CLUSTER_NAME: myCluster
    healthcheck:
        test: ["CMD", "gs_sh"]
        interval: 30s
        timeout: 10s
        retries: 5

  census-analysis: 
    build:
      context: census-analysis
      dockerfile: Dockerfile
    container_name: census-analysis
    profiles:
      - donotstart
    depends_on:
      griddb-server:
        condition: service_healthy
```

To ensure portability of the application, the Griddb and NodeJS services were containerized using Docker in such a way that the running of the NodeJS script depends on the Griddb container because without spinning up the Griddb server there will be no target syatem for the database loading operation of the NodeJS script. This means that the nodejs script was built into a docker container and then we used that to push data into the GridDB containers with the following commands:

`docker run  --network <docker-network-name> <docker-image> griddb-server:10001`

Where <docker-network-name> is the name of the Docker network you are connecting the container to. This network will be created by Docker Compose when you defined a docker-compose.yml file while <docker-image> is the name of the Docker image from which the container will be created. Ensure that this image is available locally or can be pulled from a Docker registry. Finally, griddb-server:10001 is the command and argument being passed to the container. In this case, griddb-server:10001 is a custom command or entrypoint defined within the gen image that the container will run when it starts. Here, griddb-server could refer to a command, script, or application within the container. 10001 is an argument passed to the griddb-server command, specifying a port or configuration parameter. Kindly ensure you change these values and parameters based on what is available on your Docker registry.

## Conclusion
In this project, we have successfully built a comprehensive census analytics system leveraging GridDB, Docker, and Node.js. Through this journey, we demonstrated how to set up and configure a scalable and efficient data processing pipeline capable of ingesting, analyzing, and storing census data. The combination of GridDB, Docker, and Node.js has proven to be a powerful stack for building scalable and efficient data analytics systems. By following the outlined steps and leveraging the provided Docker configurations, you can replicate and extend this solution to meet specific needs and handle various data analytics tasks.
We encourage you to explore the code repository and experiment with the setup to gain a deeper understanding of the system. With these tools and methodologies, you are well-equipped to tackle complex data analytics challenges and derive actionable insights from vast datasets. 
