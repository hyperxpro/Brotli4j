FROM arm32v7/debian:bullseye-slim

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -q -y --no-install-recommends \
        apt-transport-https \
        autoconf \
        automake \
        build-essential \
        ca-certificates \
        cmake \
        curl \
        git \
        gnupg \
        libtool \
        make \
        tar \
        unzip \
        wget \
        zip

# Eclipse Temurin 17 JDK for armhf via the Adoptium apt repo.
RUN mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
        | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bullseye main" \
        > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -q -y --no-install-recommends temurin-17-jdk && \
    rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME="/usr/lib/jvm/temurin-17-jdk-armhf"
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV JAVA_INCLUDE_PATH="${JAVA_HOME}/include"
ENV JAVA_INCLUDE_PATH2="${JAVA_HOME}/include/linux"
ENV JAVA_AWT_LIBRARY="${JAVA_HOME}/lib/arm"
ENV JAVA_JVM_LIBRARY="${JAVA_HOME}/lib/arm/server"

# Apache Maven 3.8.6
RUN cd /opt && \
    curl -fsSLO https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz && \
    tar xzf apache-maven-3.8.6-bin.tar.gz && \
    ln -s apache-maven-3.8.6 maven && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn && \
    rm apache-maven-3.8.6-bin.tar.gz
ENV MAVEN_HOME="/opt/maven"
ENV PATH="/opt/maven/bin:${PATH}"

WORKDIR /workspace
