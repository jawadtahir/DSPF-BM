FROM maven:3.6.3-openjdk-11 AS maven

COPY ./ /flink-topology
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /flink-topology

RUN mvn clean install

FROM apache/flink:1.17.1-java11

COPY --from=maven /flink-topology/target/flink-topology*.jar /opt/flink/bin/topology.jar

WORKDIR /opt/flink/bin

VOLUME /usr/lib/jvm/java-11-openjdk-amd64

ENV KAFKA_BOOTSTRAP="kafka1:9092" NUM_STREAMS="1" PG="e1"

EXPOSE 8081

ENTRYPOINT flink run --class de.tum.in.msrg.flink.FlinkWrapper topology.jar -kafka $KAFKA_BOOTSTRAP -streams $NUM_STREAMS -pg $PG