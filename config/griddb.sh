#!/bin/bash

# Set GridDB environment variables
export GS_HOME=/usr/griddb
export GS_LOG=/var/lib/griddb/log

# Set up GridDB configuration directory
mkdir -p $GS_HOME/conf

# Copy gs_node.json and gs_cluster.json from the config folder to GridDB conf directory
cp /usr/griddb/config/gs_node.json $GS_HOME/conf/gs_node.json
cp /usr/griddb/config/gs_cluster.json $GS_HOME/conf/gs_cluster.json

# Start GridDB node
gs_startnode -u admin/admin -w 0