FROM riscv64/ubuntu:22.04

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
        tar \
        unzip \
        wget \
        zip \
    && rm -rf /var/lib/apt/lists/*

# Eclipse Temurin 21 GA tarball for riscv64. Adoptium publishes GA
# riscv64 builds since 2024; we follow the API redirect to the latest
# GA at image build time. The resulting image is fixed once pushed.
RUN mkdir -p /opt/jdk && \
    curl -fsSL -o /tmp/jdk.tar.gz \
        "https://api.adoptium.net/v3/binary/latest/21/ga/linux/riscv64/jdk/hotspot/normal/eclipse?project=jdk" && \
    tar -xzf /tmp/jdk.tar.gz -C /opt/jdk --strip-components=1 && \
    rm /tmp/jdk.tar.gz
ENV JAVA_HOME="/opt/jdk"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

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
