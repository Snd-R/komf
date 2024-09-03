FROM eclipse-temurin:21-jre AS base-amd64

FROM eclipse-temurin:21-jre AS base-arm64

FROM eclipse-temurin:17-jre AS base-arm

FROM base-${TARGETARCH} AS build-final

RUN apt-get update && apt-get install -y apprise \
    && rm -rf /var/lib/apt/lists/* \

WORKDIR /app
COPY komf-app/build/libs/komf-app-1.0.0-SNAPSHOT-all.jar ./
ENV LC_ALL=en_US.UTF-8
ENV KOMF_CONFIG_DIR="/config"
ENTRYPOINT ["java","-jar", "komf-app-1.0.0-SNAPSHOT-all.jar"]
EXPOSE 8085

LABEL org.opencontainers.image.url=https://github.com/Snd-R/komf org.opencontainers.image.source=https://github.com/Snd-R/komf
