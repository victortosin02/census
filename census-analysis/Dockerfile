FROM node:18

<<<<<<< HEAD
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


=======
# Install necessary packages
RUN apt-get update && apt-get install -y curl wget

# Download and install GridDB C client
RUN wget --no-check-certificate https://github.com/griddb/c_client/releases/download/v5.6.0/griddb-c-client_5.6.0_amd64.deb
RUN dpkg -i griddb-c-client_5.6.0_amd64.deb

# Set environment variables for GridDB C client
ENV LIBRARY_PATH=$LIBRARY_PATH:/usr/lib/griddb

WORKDIR /app

# Copy project files
COPY package.json package-lock.json data-analyzer.js input.csv /app/

# Install npm packages including griddb-node-api
RUN npm install

# Set NODE_PATH environment variable for griddb-node-api
ENV NODE_PATH=/app/node_modules/griddb-node-api

# Command to run your Node.js script
ENTRYPOINT ["node", "data-analyzer.js"]
>>>>>>> 12d5334 (New Commit)
