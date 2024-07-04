ARG DOCKER_IMAGE
FROM ${DOCKER_IMAGE}
USER root

ARG TARGET_GCC_PACKAGE
ARG TARGET_GCC_EXEC

ARG CMAKE_VERSION="3.20.5"

COPY setup-repo.sh /setup-repo.sh
RUN /setup-repo.sh

RUN yum update -y && \
    yum install -y \
        "make" ${TARGET_GCC_PACKAGE} \
        "glibc" "libstdc++"

RUN update-alternatives --install /usr/bin/cc cc /usr/bin/${TARGET_GCC_EXEC}-gcc 999 && \
    update-alternatives --install /usr/bin/c++ c++ /usr/bin/${TARGET_GCC_EXEC}-g++ 999

VOLUME "/opt/java/"
ENV JAVA_HOME "/opt/java/"
ENV PATH "$PATH:$JAVA_HOME/bin/"

RUN curl -L "https://github.com/Kitware/CMake/releases/download/v${CMAKE_VERSION}/cmake-${CMAKE_VERSION}-linux-x86_64.tar.gz" -o cmake.tar.gz && \
    mkdir /opt/cmake && \
    tar -xzf cmake.tar.gz -C /opt/cmake --strip-components 1 && \
    rm cmake.tar.gz

ENV PATH "$PATH:/opt/cmake/bin/"

VOLUME "/data/"
WORKDIR "/data/"

ENTRYPOINT [ ]
CMD [ "/bin/bash" ]