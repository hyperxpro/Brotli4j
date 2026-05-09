FROM riscv64/ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -q -y --no-install-recommends \
        autoconf \
        automake \
        build-essential \
        ca-certificates \
        cmake \
        curl \
        git \
        gnupg2 \
        libtool \
        make \
        openjdk-11-jdk \
        tar \
        unzip \
        wget \
        zip \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME="/usr/lib/jvm/java-11-openjdk-riscv64"
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV JAVA_INCLUDE_PATH="${JAVA_HOME}/include"
ENV JAVA_INCLUDE_PATH2="${JAVA_HOME}/include/linux"
ENV JAVA_AWT_LIBRARY="${JAVA_HOME}/lib"
ENV JAVA_JVM_LIBRARY="${JAVA_HOME}/lib/server"

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
