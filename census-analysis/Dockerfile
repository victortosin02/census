FROM node:18

# Install GridDB C Client
WORKDIR /
RUN apt-get update && apt-get install -y wget
RUN wget --no-check-certificate https://github.com/griddb/c_client/releases/download/v5.6.0/griddb-c-client_5.6.0_amd64.deb
RUN dpkg -i griddb-c-client_5.6.0_amd64.deb || apt-get install -f -y

# Set up the Node.js application
WORKDIR /app
COPY package.json /app/package.json
COPY package-lock.json /app/package-lock.json
COPY data-analyzer.js /app/data-analyzer.js
COPY input.csv /app/input.csv

# Install Node.js dependencies
RUN npm install

# Define the entry point
ENTRYPOINT ["node", "data-analyzer.js"]
