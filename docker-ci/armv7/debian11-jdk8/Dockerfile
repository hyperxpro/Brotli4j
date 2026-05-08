FROM arm32v7/debian:bullseye-slim

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

# Azul Zulu Embedded JDK 8 for armhf. Corretto does not ship for armv7,
# so we pin the same Zulu build the inline pipeline used previously.
RUN mkdir -p /opt/jdk && \
    curl -fsSL -o /tmp/zulu.tar.gz \
        https://cdn.azul.com/zulu-embedded/bin/zulu8.66.0.15-ca-jdk8.0.352-linux_aarch32hf.tar.gz && \
    tar -xzf /tmp/zulu.tar.gz -C /opt/jdk --strip-components=1 && \
    rm /tmp/zulu.tar.gz
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
