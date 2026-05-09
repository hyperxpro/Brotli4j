FROM arm64v8/ubuntu:20.04

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

# Amazon Corretto 21 via jabba (matches the original install: block,
# which used distro: ubuntu20.04 only for JDK 21).
RUN curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | \
        JABBA_COMMAND="install 21.0.1-custom=tgz+https://corretto.aws/downloads/resources/21.0.1.12.1/amazon-corretto-21.0.1.12.1-linux-aarch64.tar.gz -o /jdk" bash
ENV JAVA_HOME="/jdk"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /workspace
