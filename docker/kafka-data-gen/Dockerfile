FROM maven:3.6.3-openjdk-11 AS maven

COPY ./ /kafka-data-gen
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /kafka-data-gen

RUN mvn package

FROM openjdk:11-jre-slim

COPY --from=maven /kafka-data-gen/target/kafka-data-gen*.jar /jar/datagen.jar
WORKDIR /jar

ENV KAFKA_BOOTSTRAP="kafka:9092"
ENV KAFKA_TOPIC="input"
ENV DELAY_COUNT=1000

ENTRYPOINT java -jar datagen.jar -kafka $KAFKA_BOOTSTRAP -topic $KAFKA_TOPIC -delay $DELAY_COUNT
