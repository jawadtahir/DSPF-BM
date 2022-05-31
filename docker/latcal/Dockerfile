FROM maven:3.6.3-openjdk-11 AS maven

COPY ./ /latcal
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /latcal

RUN mvn package

FROM openjdk:11-jre-slim

COPY --from=maven /latcal/target/latcal*.jar /jar/latcal.jar
COPY --from=maven /latcal/target/classes/log4j2.xml /jar/log4j2.xml
WORKDIR /jar

ENV KAFKA_BOOTSTRAP="kafka:9092"

EXPOSE 52923

ENTRYPOINT java -jar latcal.jar -kafka $KAFKA_BOOTSTRAP