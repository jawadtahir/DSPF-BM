FROM maven:3.6.3-openjdk-11-slim AS maven

COPY ./ /storm-topology
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /storm-topology

RUN mvn package

FROM storm:2.4.0
COPY --from=maven /storm-topology/target/storm-topology*.jar /apache-storm-2.4.0/bin/storm-topology.jar

ENV NIMBUS_SEED=nimbus
ENV KAFKA_SEED=kafka:9092
ENV KAFKA_INPUT_TOPIC=input
ENV KAFKA_OUTPUT_TPOIC=output

ENTRYPOINT storm jar /apache-storm-2.4.0/bin/storm-topology.jar de.tum.in.msrg.storm.Submitter -nimbus $NIMBUS_SEED -kafka $KAFKA_SEED -input $KAFKA_INPUT_TOPIC -output $KAFKA_OUTPUT_TPOIC