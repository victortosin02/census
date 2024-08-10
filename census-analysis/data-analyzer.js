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