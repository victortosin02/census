const griddb = require('griddb-node-api');

async function testConnection() {
    try {
        const factory = griddb.StoreFactory.getInstance();
        const store = await factory.getStore({
            notificationMember: '127.0.0.1:10001',
            clusterName: 'myCluster',
            username: 'admin',
            password: 'admin'
        });
        console.log('Connection successful.');
    } catch (error) {
        console.error('Connection error:', error);
    }
}

testConnection();