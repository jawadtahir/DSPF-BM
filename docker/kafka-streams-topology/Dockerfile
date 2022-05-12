FROM maven:3.6.3-openjdk-11-slim AS maven

COPY ./ /kafka-topology
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /kafka-topology

RUN mvn package

FROM openjdk:11-jdk-slim

COPY --from=maven /kafka-topology/target/kafka-streams-topology*.jar /kafka-topology/kafka-streams-topology.jar
WORKDIR /kafka-topology

ENV KAFKA_BOOTSTRAP="kafka1:9092"
ENV INPUT_TOPIC="input"
ENV OUTPUT_TOPIC="output"
ENV PROCESSING_GUARANTEE="exactly_once_beta"
ENV JMX_PORT=52923

ENTRYPOINT java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=$JMX_PORT  -jar kafka-streams-topology.jar -kafka $KAFKA_BOOTSTRAP -input $INPUT_TOPIC -output $OUTPUT_TOPIC -guarantee $PROCESSING_GUARANTEE

#-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=52923