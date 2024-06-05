ARG UBUNTU_VERSION
FROM ubuntu:${UBUNTU_VERSION}
USER root

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