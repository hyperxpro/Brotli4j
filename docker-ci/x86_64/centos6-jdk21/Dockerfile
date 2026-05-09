# CentOS 6 base is intentional — keeps glibc 2.12 ABI floor for the
# native .so so it loads on RHEL 6+, Ubuntu 14.04+, etc. Do not bump
# the base image without coordinating an ABI break with downstream.
FROM gatlingcorp/centos:6-gcc5

RUN rm -vf /usr/lib64/libstdc++.so.6.0.21-gdb.py

RUN yum -y install git && yum clean all

# Amazon Corretto JDK 21 via jabba (tarball install into /jdk).
# Corretto's RPM repo doesn't ship for CentOS 6 anymore, so use the
# tarball release pinned to a specific build for determinism.
RUN curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | \
        JABBA_COMMAND="install 21.0.1-custom=tgz+https://corretto.aws/downloads/resources/21.0.1.12.1/amazon-corretto-21.0.1.12.1-linux-x64.tar.gz -o /jdk" bash
ENV JAVA_HOME="/jdk"
ENV PATH="/jdk/bin:${PATH}"

# Apache Maven 3.8.6
RUN cd /opt && \
    wget --no-verbose https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz && \
    tar xzf apache-maven-3.8.6-bin.tar.gz && \
    ln -s apache-maven-3.8.6 maven && \
    ln -s /opt/maven/bin/mvn /usr/bin/mvn && \
    rm apache-maven-3.8.6-bin.tar.gz
ENV MAVEN_HOME="/opt/maven"
ENV PATH="/opt/maven/bin:${PATH}"

# CMake 3.20.1 (CentOS 6 yum cmake is too old)
RUN cd /opt && \
    wget --no-verbose https://github.com/Kitware/CMake/releases/download/v3.20.1/cmake-3.20.1-linux-x86_64.sh && \
    chmod a+x cmake-3.20.1-linux-x86_64.sh && \
    yes | ./cmake-3.20.1-linux-x86_64.sh && \
    rm cmake-3.20.1-linux-x86_64.sh
ENV PATH="/opt/cmake-3.20.1-linux-x86_64/bin:${PATH}"

# JDK 17+ uses lib/server (no jre/) — keep both paths just in case
RUN echo "/jdk/lib" >> /etc/ld.so.conf && \
    echo "/jdk/lib/server" >> /etc/ld.so.conf && \
    ldconfig

WORKDIR /workspace
