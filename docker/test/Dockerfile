ARG DOCKER_IMAGE
FROM ${DOCKER_IMAGE}
USER root

RUN apt-get update -y && \
    apt-get install -y \
        "libc6" "libstdc++6" "libatomic1" "zlib1g" && \
    apt-get clean

VOLUME "/opt/java/"
ENV JAVA_HOME "/opt/java/"
ENV PATH "$PATH:$JAVA_HOME/bin/"

VOLUME "/data"
WORKDIR "/data"

ENTRYPOINT [ ]
CMD [ "/bin/bash" ]