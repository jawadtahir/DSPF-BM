FROM maven:3.6.3-openjdk-11 AS maven

COPY ./ /kafka-data-gen
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /kafka-data-gen

RUN mvn package

FROM openjdk:11-jre-slim

COPY --from=maven /kafka-data-gen/target/kafka-data-gen*.jar /jar/datagen.jar
#COPY --from=maven /kafka-data-gen/target/classes/log4j2.xml /jar/log4j2.xml
WORKDIR /jar
VOLUME /reports

ENV KAFKA_BOOTSTRAP="kafka1:9092" DELAY_COUNT=1000 DELAY_LENGTH=1 EVENTS_PER_WINDOW=5000 BENCHMARK_LENGTH=180 NUM_PRODUCERS=1 NUM_STREAMS=1

EXPOSE 52923

ENTRYPOINT exec  java -XX:ParallelGCThreads=4 -XX:ConcGCThreads=4 -Xmx15g -cp *:. de.tum.in.msrg.utils.Runner -benchmarkLength $BENCHMARK_LENGTH -kafka $KAFKA_BOOTSTRAP -producers $NUM_PRODUCERS -streams $NUM_STREAMS -delay $DELAY_COUNT -length $DELAY_LENGTH -events $EVENTS_PER_WINDOW
