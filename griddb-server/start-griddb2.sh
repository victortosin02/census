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
    jq 'del(.cluster.notificationAddress, .cluster.notificationPort)' /var/lib/gridstore/conf/gs_cluster.json > tmp.json

    # Config notification member for Fixed_List method
    jq --arg ip "$ip_address" '.cluster.notificationMember = [{"cluster":{"address":$ip, "port":10010}, "sync":{"address":$ip,"port":10020}, "system":{"address":$ip, "port":10040}, "transaction":{"address":$ip, "port":10001}, "sql":{"address":$ip, "port":20001}}]' tmp.json > tmp_gs_cluster.json
    mv tmp_gs_cluster.json /var/lib/gridstore/conf/gs_cluster.json
    rm tmp.json
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
        sed -i "s/\"clusterName\":\"\"/\"clusterName\":\"$GRIDDB_CLUSTER_NAME\"/" /var/lib/gridstore/conf/gs_cluster.json

        # MULTICAST mode
        if [ ! -z $NOTIFICATION_ADDRESS ]; then
            echo "MULTICAST mode address"
            sed -i "s/\"notificationAddress\":\"239.0.0.1\"/\"notificationAddress\":\"$NOTIFICATION_ADDRESS\"/" /var/lib/gridstore/conf/gs_cluster.json
        fi

        if [ ! -z $NOTIFICATION_PORT ]; then
            echo "MULTICAST mode port"
            sed -i "s/\"notificationPort\":31999/\"notificationPort\":$NOTIFICATION_PORT/" /var/lib/gridstore/conf/gs_cluster.json
        fi

        # FIXED_LIST mode
        if [ ! -z $NOTIFICATION_MEMBER ]; then
            echo "Fixed List mode"
            if [ $NOTIFICATION_MEMBER != 1 ]; then
                echo "$NOTIFICATION_MEMBER invalid. Fixed list GridDB CE mode supports one member, please check again!"
                exit 1
            fi
            if grep -q 'notificationMember' /var/lib/gridstore/conf/gs_cluster.json; then
                get_ipadress
                fixlist_config
            fi
        fi

        # PROVIDER mode
        if [ ! -z $NOTIFICATION_PROVIDER ]; then
            echo "Provider mode hasn't supported"
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
