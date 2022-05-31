FROM maven:3.6.3-openjdk-11 AS maven

COPY ./ /kafka-data-gen
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /kafka-data-gen

RUN mvn package

FROM openjdk:11-jre-slim

COPY --from=maven /kafka-data-gen/target/kafka-data-gen*.jar /jar/datagen.jar
COPY --from=maven /kafka-data-gen/target/classes/log4j2.xml /jar/log4j2.xml
WORKDIR /jar

ENV KAFKA_BOOTSTRAP="kafka:9092" DELAY_COUNT=1000

EXPOSE 52923

ENTRYPOINT java -jar datagen.jar -kafka $KAFKA_BOOTSTRAP -delay $DELAY_COUNT
