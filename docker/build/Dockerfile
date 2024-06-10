ARG DOCKER_IMAGE
FROM ${DOCKER_IMAGE}
USER root

ARG TARGET_GCC_PACKAGE
ARG TARGET_GCC_EXEC

RUN apt-get update -y && \
    apt-get install -y \
        "make" "cmake" ${TARGET_GCC_PACKAGE} \
        "libc6" "libstdc++6" "libatomic1" "zlib1g" && \
    apt-get clean

RUN update-alternatives --install /usr/bin/cc cc /usr/bin/${TARGET_GCC_EXEC}-gcc 999 && \
    update-alternatives --install /usr/bin/c++ c++ /usr/bin/${TARGET_GCC_EXEC}-g++ 999

VOLUME "/opt/java/"
ENV JAVA_HOME "/opt/java/"
ENV PATH "$PATH:$JAVA_HOME/bin/"

VOLUME "/data"
WORKDIR "/data"

ENTRYPOINT [ ]
CMD [ "/bin/bash" ]