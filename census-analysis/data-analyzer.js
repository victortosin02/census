const fs = require('fs');
const csv = require('csv-parser');
const griddb = require('griddb-node-api');

const containerName = "census-analysis";

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
async function processData(data) {
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

// Initialize GridDB Store
const initStore = async() => {
    const factory = griddb.StoreFactory.getInstance();
    try {
        const store = await factory.getStore({
            notificationMember: "127.0.0.1:10001",
            clusterName: "myCluster",
            username: "admin",
            password: "admin",
        });
        return store;
    } catch (e) {
        console.error("Error initializing GridDB store:", e);
        throw e;
    }
};

// Initialize Container Info
function initContainer() {
    const conInfo = new griddb.ContainerInfo({
        name: containerName,
        columnInfoList: [
            ["occupation", griddb.Type.STRING],
            ["minIncome", griddb.Type.LONG],
            ["maxIncome", griddb.Type.LONG],
            ["minFamilySize", griddb.Type.INTEGER],
            ["maxFamilySize", griddb.Type.INTEGER]
        ],
        type: griddb.ContainerType.COLLECTION,
        rowKey: true,
    });

    return conInfo;
}

// Insert Data into GridDB
async function insertIntoGridDB(data, store, conInfo) {
    try {
        // Check if the container exists
        let container;

        // Create a new container
        console.log(`New container created '${conInfo.name}'.`);
        container = await store.putContainer(conInfo);

        // Insert data into the container one by one
        for (const [occupation, stats] of Object.entries(data)) {
            const rowData = [
                occupation,
                stats.minIncome,
                stats.maxIncome,
                stats.minFamilySize,
                stats.maxFamilySize
            ];

            const result = await insert(rowData, container);
            if (result.status) {
                console.log(`Data for occupation '${occupation}' successfully inserted.`);
            } else {
                console.error(`Error inserting data for occupation '${occupation}':`, result.error);
            }
        }
    } catch (error) {
        console.error('Error during GridDB operations:', error);
        throw error;
    }
}


// Main function
async function main() {
    try {
        const csvFilePath = 'input.csv';
        const data = await readCSV(csvFilePath);
        const processedData = processData(data);
        console.log('Processed Data:', processedData);

        // Initialize GridDB and Container within the main function
        const store = await initStore();
        const conInfo = initContainer();
        await insertIntoGridDB(processedData, store, conInfo);
    } catch (error) {
        console.error('Error in main function:', error);
    }
}

main();