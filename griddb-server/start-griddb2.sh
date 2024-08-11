#!/bin/bash

if [ "${1:0:1}" = '-' ]; then
    set -- griddb "$@"
fi

# Save variable and value to config file
save_config() {
    echo "GRIDDB_CLUSTER_NAME=\"$GRIDDB_CLUSTER_NAME\"" >> /var/lib/gridstore/conf/gridstore.conf
    echo "GRIDDB_USERNAME=\"$GRIDDB_USERNAME\""         >> /var/lib/gridstore/conf/gridstore.conf
    echo "GRIDDB_PASSWORD=\"$GRIDDB_PASSWORD\""         >> /var/lib/gridstore/conf/gridstore.conf
}

# Get IP Address
get_ipadress() {
    # Get IP address of machine
    ip_address=$(hostname -I | awk '{print $1}')
}


# Config for fixed_list method
fixlist_config() {
    # Remove "notificationAddress" and "notificationPort"
    jq 'del(.cluster.notificationAddress) | del(.cluster.notificationPort)' /var/lib/gridstore/conf/gs_cluster.json | tee tmp.json > /dev/null

    # Config notification member for Fixed_List method
    jq '.cluster |= .+ {"notificationMember": [{"cluster":{"address", "port":10010}, "sync":{"address","port":10020}, "system":{"address", "port":10040}, "transaction":{"address", "port":10001}, "sql":{"address", "port":20001}}]}' tmp.json | tee tmp_gs_cluster.json >/dev/null
    mv tmp_gs_cluster.json /var/lib/gridstore/conf/gs_cluster.json
    rm tmp.json

    # Add serviceAddress for Fixed_List method
    jq --arg ip_address "$ip_address"  '. + { serviceAddress: $ip_address}'  /var/lib/gridstore/conf/gs_node.json  >  tmp_gs_node.json
    mv tmp_gs_node.json /var/lib/gridstore/conf/gs_node.json

    # Set IP address
    sed -i -e s/\"address\":\ null/\"address\":\"$ip_address\"/g \/var/lib/gridstore/conf/gs_cluster.json
}


# First parameter after run images
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
        # Extra modification based on environment variable
        gs_passwd $GRIDDB_USERNAME -p $GRIDDB_PASSWORD
        sed -i -e s/\"clusterName\":\"\"/\"clusterName\":\"$GRIDDB_CLUSTER_NAME\"/g \/var/lib/gridstore/conf/gs_cluster.json


        # # Compression Mode
        if [ ! -z $COMPRESSION_MODE ]; then
            echo "Compression mode change"
            if [ $COMPRESSION_MODE -eq 1 ]; then 
                sed -i -e 's/NO_COMPRESSION/COMPRESSION_ZLIB/' \/var/lib/gridstore/conf/gs_node.json
            elif [ $COMPRESSION_MODE -eq 2 ]; then
                sed -i -e 's/NO_COMPRESSION/COMPRESSION_ZSTD/' \/var/lib/gridstore/conf/gs_node.json
            fi
        fi

        # MULTICAST mode
        if [ ! -z $NOTIFICATION_ADDRESS ]; then
            echo "MULTICAST mode address"
            sed -i -e s/\"notificationAddress\":\"239.0.0.1\"/\"notificationAddress\":\"$NOTIFICATION_ADDRESS\"/g \/var/lib/gridstore/conf/gs_cluster.json
        fi

        if [ ! -z $NOTIFICATION_PORT ]; then
            echo "MULTICAST mode port"
            sed -i -e s/\"notificationPort\":31999/\"notificationPort\":$NOTIFICATION_PORT/g \/var/lib/gridstore/conf/gs_cluster.json
        fi

        # FIXED_LIST mode
        if [ ! -z $NOTIFICATION_MEMBER ]; then
            echo "Fixed List mode"
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

        # PROVIDER mode
        if [ ! -z $NOTIFICATION_PROVIDER ]; then
            echo "Provider mode haven't support"
            exit 1
        fi

        # Write to config file
        save_config
    fi

    # Read config file
    . /var/lib/gridstore/conf/gridstore.conf

    # Start service
    cd /var/lib/gridstore
    sleep 5 && gs_joincluster -u $GRIDDB_USERNAME/$GRIDDB_PASSWORD -c $GRIDDB_CLUSTER_NAME -w &
    gsserver --conf ./conf
fi
exec "$@"
