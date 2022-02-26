FROM eclipse-temurin:11-jre
VOLUME /tmp
WORKDIR app
COPY build/libs/komf-1.0-SNAPSHOT-all.jar ./
ENV LC_ALL=en_US.UTF-8
ENV KOMF_CONFIGDIR="/config/application.yml"
ENTRYPOINT ["java","-jar", "komf-1.0-SNAPSHOT-all.jar"]
EXPOSE 8085
