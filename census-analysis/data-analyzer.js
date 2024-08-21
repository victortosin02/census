const fs = require('fs');
const csv = require('csv-parser');
const griddb = require('griddb-node-api');

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
        const familySize = parseInt(row.family_size, 10);
        const income = parseInt(row.income, 10);

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
    try {
        const factory = griddb.StoreFactory.getInstance();
        const store = await factory.getStore({
            "notificationMember": ['127.0.0.1:10001'],
            "clusterName": "myCluster",
            "username": "admin",
            "password": "admin"
        });

        const containerName = 'occupation_statistics';
        const containerInfo = new griddb.ContainerInfo({
            name: containerName,
            columnInfoList: [
                ['id', griddb.Type.INTEGER],
                ["occupation", griddb.Type.STRING],
                ["minIncome", griddb.Type.LONG],
                ["maxIncome", griddb.Type.LONG],
                ["minFamilySize", griddb.Type.INTEGER],
                ["maxFamilySize", griddb.Type.INTEGER]
            ],
            'type': griddb.ContainerType.COLLECTION,
            'rowKey': true
        });

        return containerInfo

        let container;
        try {
            container = await store.getContainer(containerName);
            console.log(`Container '${containerName}' already exists.`);
        } catch (e) {
            console.log(`Creating new container '${containerName}'.`);
            container = await store.putContainer(containerInfo);
            await container.createIndex({
                'columnName': 'id',
                'indexType': griddb.IndexType.DEFAULT
            });
        }

        let id = 0;
        const rows = [];
        for (const [occupation, stats] of Object.entries(data)) {
            id++;
            rows.push([
                id,
                occupation,
                stats.minIncome,
                stats.maxIncome,
                stats.minFamilySize,
                stats.maxFamilySize
            ]);
        }

        await container.multiPut(rows);
        console.log('Data successfully inserted into GridDB.');

    } catch (error) {
        console.error('Error during GridDB operations:', error);
        throw error; // Re-throw to ensure main catches it
    }
}

// Main function
async function main() {
    try {
        const csvFilePath = 'input.csv';
        const data = await readCSV(csvFilePath);
        const processedData = processData(data);
        console.log('Processed Data:', processedData); // Debugging statement
        await insertIntoGridDB(processedData);
    } catch (error) {
        console.error('Error in main function:', error);
    }
}

main();