ARG UBUNTU_IMAGE
FROM ${UBUNTU_IMAGE}
USER root

RUN apt-get update -y && \
    apt-get install -y \
        libc6 libstdc++6 libatomic1 zlib1g && \
    apt-get clean

VOLUME "/opt/java"
ENV JAVA_HOME="/opt/java"
ENV PATH="$JAVA_HOME:$PATH"

VOLUME "/opt/maven"
ENV MAVEN_HOME="/opt/maven"
ENV PATH="$MAVEN_HOME:$PATH"

VOLUME "/data"
WORKDIR "/data"

ENTRYPOINT [ ]
CMD [ "/bin/bash" ]