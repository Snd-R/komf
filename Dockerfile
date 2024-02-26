FROM eclipse-temurin:21-jre AS base-amd64

FROM eclipse-temurin:21-jre AS base-arm64

FROM eclipse-temurin:17-jre AS base-arm

FROM base-${TARGETARCH} AS build-final
VOLUME /tmp
WORKDIR app
COPY build/libs/komf-1.0-SNAPSHOT-all.jar ./
ENV LC_ALL=en_US.UTF-8
ENV KOMF_CONFIG_DIR="/config"
ENTRYPOINT ["java","-jar", "komf-1.0-SNAPSHOT-all.jar"]
EXPOSE 8085
