# Dockerfile for GridDB Server
FROM centos:7

# Use a specific mirror to avoid DNS issues (if needed)
RUN sed -i 's|^mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/CentOS-Base.repo && \
    sed -i 's|^#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-Base.repo

# Install necessary dependencies for GridDB and Java
RUN yum -y update && \
    yum -y install epel-release && \
    yum -y install java-1.8.0-openjdk wget tar coreutils

### Install GridDB ###
# Copy RPM files to the container
COPY rpm/griddb-5.6.0-linux.x86_64.rpm /tmp/

# Install GridDB
RUN yum -y install /tmp/griddb-5.6.0-linux.x86_64.rpm && \
    rm /tmp/griddb-5.6.0-linux.x86_64.rpm

# Ensure gs_startnode is executable
RUN chmod +x /usr/griddb-5.6.0/bin/gs_startnode || true

# Set environment variables for GridDB
ENV GS_HOME=/usr/griddb-5.6.0
ENV GS_LOG=/var/lib/griddb/log

# Create necessary directories for GridDB
RUN mkdir -p $GS_HOME $GS_LOG

# Expose necessary ports for GridDB
EXPOSE 10001 10010

# Copy GridDB configuration files
COPY conf/gs_node.json $GS_HOME/conf/gs_node.json

# Start GridDB using entrypoint script
COPY entrypoint_server.sh /entrypoint_server.sh
RUN chmod +x /entrypoint_server.sh

ENTRYPOINT ["/entrypoint_server.sh"]
