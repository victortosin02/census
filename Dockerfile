# Dockerfile for GridDB Server, Client, and Hadoop
FROM centos:7

# Use a specific mirror to avoid DNS issues (if needed)
RUN sed -i 's|^mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/CentOS-Base.repo && \
    sed -i 's|^#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-Base.repo

# Install necessary packages
RUN yum install -y hostname wget

### Install GridDB ###
# Copy RPM file to the container (ensure correct path)
COPY rpm/griddb-5.6.0-linux.x86_64.rpm /tmp/

# Install GridDB and remove the RPM file
RUN yum -y install /tmp/griddb-5.6.0-linux.x86_64.rpm && \
    rm /tmp/griddb-5.6.0-linux.x86_64.rpm

# Set environment variables for GridDB
ENV GS_HOME=/usr/griddb
ENV GS_LOG=/var/lib/griddb/log

# Create necessary directories for GridDB
RUN mkdir -p $GS_HOME $GS_LOG $GS_HOME/conf

# Change password for GridDB admin user
RUN su - gsadm -c "gs_passwd admin -p admin"

### Install Java JDK 17 ###
# Download and install Java JDK 17
RUN wget https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz -O /tmp/jdk-17_linux-x64_bin.tar.gz && \
    tar -xzvf /tmp/jdk-17_linux-x64_bin.tar.gz -C /usr/local && \
    rm /tmp/jdk-17_linux-x64_bin.tar.gz && \
    ln -s /usr/local/jdk-17 /usr/local/jdk

# Set environment variables for Java
ENV JAVA_HOME=/usr/local/jdk
ENV PATH=$JAVA_HOME/bin:$PATH

# Copy griddb.sh script from the config folder
COPY config/griddb.sh /usr/local/bin/griddb.sh
RUN chmod +x /usr/local/bin/griddb.sh

# Copy gs_node.json and gs_cluster.json to GridDB configuration directory
COPY config/gs_node.json $GS_HOME/conf/gs_node.json
COPY config/gs_cluster.json $GS_HOME/conf/gs_cluster.json

### Install Hadoop ###
# Set Hadoop version
ENV HADOOP_VERSION=3.2.4

# Download and install Hadoop
RUN wget https://downloads.apache.org/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz && \
    tar -xzvf hadoop-$HADOOP_VERSION.tar.gz && \
    mv hadoop-$HADOOP_VERSION /usr/local/hadoop && \
    rm hadoop-$HADOOP_VERSION.tar.gz

# Set environment variables for Hadoop
ENV HADOOP_HOME=/usr/local/hadoop
ENV PATH=$HADOOP_HOME/bin:$PATH

# Expose necessary ports for GridDB (adjust as per GridDB requirements)
EXPOSE 10001 10010

# Start GridDB service using the CMD instruction
CMD ["/bin/bash", "-c", "/usr/local/bin/griddb.sh && tail -f /dev/null"]
