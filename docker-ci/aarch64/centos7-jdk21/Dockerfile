FROM arm64v8/centos:centos7.9.2009

# CentOS 7 reached EOL on 2024-06-30 and the public mirrors are dead.
# Rewrite all CentOS-*.repo files to point at vault.centos.org.
RUN sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*.repo && \
    sed -i 's|#baseurl=http://mirror.centos.org|baseurl=https://vault.centos.org|g' /etc/yum.repos.d/CentOS-*.repo

# Build dependencies
RUN yum install -y \
        apr-devel \
        autoconf \
        automake \
        gcc \
        gcc-c++ \
        git \
        glibc-devel \
        libtool \
        lksctp-tools \
        lsb-core \
        make \
        openssl-devel \
        tar \
        unzip \
        wget \
        zip \
    && yum clean all && rm -rf /var/cache/yum

# CMake 3.20.1 (yum on CentOS 7 ships only cmake 2.8)
RUN cd /opt && \
    wget --no-verbose https://github.com/Kitware/CMake/releases/download/v3.20.1/cmake-3.20.1-linux-aarch64.sh && \
    chmod a+x cmake-3.20.1-linux-aarch64.sh && \
    yes | ./cmake-3.20.1-linux-aarch64.sh && \
    rm cmake-3.20.1-linux-aarch64.sh
ENV PATH="/opt/cmake-3.20.1-linux-aarch64/bin:${PATH}"

# Apache Maven 3.8.6
RUN cd /opt && \
    wget --no-verbose https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz && \
    tar xzf apache-maven-3.8.6-bin.tar.gz && \
    ln -s apache-maven-3.8.6 maven && \
    rm apache-maven-3.8.6-bin.tar.gz
ENV MAVEN_HOME="/opt/maven"
ENV PATH="/opt/maven/bin:${PATH}"

# Amazon Corretto JDK 21
RUN rpm --import https://yum.corretto.aws/corretto.key && \
    curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo && \
    yum install -y java-21-amazon-corretto-devel && \
    yum clean all && rm -rf /var/cache/yum
ENV JAVA_HOME="/usr/lib/jvm/java-21-amazon-corretto"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /workspace
