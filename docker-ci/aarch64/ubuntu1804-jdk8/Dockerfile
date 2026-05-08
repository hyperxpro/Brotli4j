FROM arm64v8/ubuntu:18.04

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

# Amazon Corretto 8 via jabba (matches the original install: block).
RUN curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | \
        JABBA_COMMAND="install 1.8.0-custom=tgz+https://corretto.aws/downloads/resources/8.352.08.1/amazon-corretto-8.352.08.1-linux-aarch64.tar.gz -o /jdk" bash
ENV JAVA_HOME="/jdk"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /workspace
