## Introduction
In this project, we will be concentrating on census analytics system designed to ingest data data from a source system, analyze and generate insights from census data, and finally store the processed data in a target syatem. Census data in this case refers to the demographic information retrieved from a population at regular intervals, typically conducted by governments to gather information about the population's characteristics, such as age, gender, race, income, education level, household composition, and more. The census analytics system has proved efficient where census officials can upload census data from household enumeration and insights are generated after the processing operations on this data. To narrow this done in terms of deliverables, this project seeks to analyze and calculate the minimum and maximum income and family size for each occupation and store the results in a GridDB database leveraging on stacks of technologies such as Node JS, JDBC, Docker and GridDB.

We will explore one of Griddb v5.6 new feature to handle scalabilty as data volume increases considering possibility of large volume owing to the fact we are worlking on macro-economic data. With this new feature, we can provision for scalability with this new version's compression algorithm called COMPRESSION_ZSTD availed to us in the gs_node.json config file.

## Methodology
The purpose of this project is to analyze sample data of households from an enumeration exercise to a source system, analyze the ingested data and load analyzed data to a database. To accomplish this, we will be leveraging seems Docker because it makes it easy to spin up all the required services.

##  How to Follow Along
If you plan to code along yourself while you read this article, you can grab the source code from the repo below:

`git clone https://github.com/victortosin02/census`

Once you have cloned the repo or downloaded the project folders and files, you need to build and start the griddb server and later get into the data analzyer ad]nd laoding script to push rows of data into the provisioned Griddb server.

To get spin up the application, simply run:

`docker-compose up --build`

This starts the griddb and census-analyzer containers in the docker.yaml file:

## Implementation
From the implementation end, a node.js script has been provided called data-analyzer.js that ingest data from a csv file, analyzes and calculate the minimum and maximum income and family size for each occupation and store the results in GridDB database. To ensure portability of the application, the Griddb and NodeJS services were containerized using Docker in such a way that the running of the NodeJS script depends on the running of the Griddb container. This means that the nodejs script was built into a docker container and then we used that to push data into the GridDB containers with the following commands:

`docker build -t data-analyzer .`
`docker run  --network docker-griddb_default gen griddb-server3:10001`

Below is the NodeJS script responsible for ingesting, analyzing and loading the data to the griddb database:

`
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
`



