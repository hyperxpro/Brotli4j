FROM arm32v7/ubuntu:18.04

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

# Azul Zulu 8 armhf via jabba (matches the original install: block).
RUN curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | \
        JABBA_COMMAND="install 1.8.0-custom=tgz+https://cdn.azul.com/zulu-embedded/bin/zulu8.66.0.15-ca-jdk8.0.352-linux_aarch32hf.tar.gz -o /jdk" bash
ENV JAVA_HOME="/jdk"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /workspace
