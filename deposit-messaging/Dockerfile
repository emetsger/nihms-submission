FROM openjdk:8u151-jdk-alpine3.7

ARG SPRING_ACTIVEMQ_BROKER_URL
ARG PASS_FEDORA_BASEURL
ARG PASS_FEDORA_USER
ARG PASS_FEDORA_PASSWORD
ARG PASS_ELASTICSEARCH_URL
ARG PASS_ELASTICSEARCH_LIMIT
ARG PASS_DEPOSIT_TRANSPORT_CONFIGURATION
ARG FCREPO_HOST
ARG FCREPO_PORT
ARG FCREPO_JMS_PORT
ARG DSPACE_HOST
ARG DSPACE_PORT
ARG FTP_HOST
ARG FTP_PORT
ARG ES_HOST
ARG ES_PORT
ARG DEPOSIT_DEBUG_PORT

ENV DEPOSIT_SERVICES_VERSION=${project.version} \
    JSONLD_CONTEXT_VERSION=3.2 \
    FCREPO_HOST=${FCREPO_HOST:-fcrepo} \
    FCREPO_PORT=${FCREPO_PORT:-8080} \
    FCREPO_JMS_PORT=${FCREPO_JMS_PORT:-61616} \
    DSPACE_HOST=${DSPACE_HOST:-dspace} \
    DSPACE_PORT=${DSPACE_PORT:-8181} \
    FTP_HOST=${FTP_HOST:-ftpserver} \
    FTP_PORT=${FTP_PORT:-21} \
    ES_HOST=${ES_HOST:-elasticsearch} \
    ES_PORT=${ES_PORT:-9200} \
    DEPOSIT_DEBUG_PORT=${DEPOSIT_DEBUG_PORT:-5007} \
    PASS_FEDORA_USER=${PASS_FEDORA_USER:-fedoraAdmin} \
    PASS_FEDORA_PASSWORD=${PASS_FEDORA_PASSWORD:-moo} \
    PASS_ELASTICSEARCH_LIMIT=${PASS_ELASTICSEARCH_LIMIT:-100} \
    PASS_DEPOSIT_REPOSITORY_CONFIGURATION=${PASS_DEPOSIT_REPOSITORY_CONFIGURATION:-classpath:/repositories.json}

ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DEPOSIT_DEBUG_PORT}

EXPOSE ${DEPOSIT_DEBUG_PORT}
COPY ${project.basedir}/src/main/docker/bin/aws_entrypoint.sh /bin/
RUN apk update && \
    apk add --no-cache ca-certificates wget python py-pip && \
    pip install awscli && \
    chmod 700 /bin/aws_entrypoint.sh && \
    mkdir /app

COPY ${project.build.directory}/${project.artifactId}-${project.version}-exec.jar .

WORKDIR /app

RUN jar -xf /${project.artifactId}-${project.version}-exec.jar && \
    rm /${project.artifactId}-${project.version}-exec.jar

ENTRYPOINT [ "java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "org.springframework.boot.loader.JarLauncher" ]
CMD [ "listen" ]