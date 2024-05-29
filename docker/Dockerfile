FROM alpine:latest

RUN apk update && \
    apk add binutils gcc g++ gcompat curl tar && \
    apk cache clean

RUN mkdir -p /opt/java
VOLUME "/opt/java"

ENV JAVA_HOME=/opt/java
ENV PATH=/opt/java/bin:$PATH

RUN mkdir -p /opt/maven
RUN curl "https://dlcdn.apache.org/maven/maven-3/3.9.7/binaries/apache-maven-3.9.7-bin.tar.gz" -o /opt/maven.tar.gz && \
    tar -xf /opt/maven.tar.gz -C /opt/maven --strip-components 1 && \
    rm /opt/maven.tar.gz

ENV MAVEN_HOME=/opt/maven
ENV PATH=/opt/maven/bin:$PATH

ENTRYPOINT []
CMD []