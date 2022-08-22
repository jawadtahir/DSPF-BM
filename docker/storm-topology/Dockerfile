FROM maven:3.6.3-openjdk-11-slim AS maven

COPY ./ /storm-topology
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /storm-topology

RUN mvn package

FROM storm:2.4.0
COPY --from=maven /storm-topology/target/storm-topology*.jar /apache-storm-2.4.0/bin/storm-topology.jar
#COPY ./storm-metrics-reporter-prometheus.jar /apache-storm-2.4.0/extlib/storm-metrics-reporter-prometheus.jar
#COPY ./storm-metrics-reporter-prometheus.jar /apache-storm-2.4.0/extlib-daemon/storm-metrics-reporter-prometheus.jar
COPY ./jmx_prometheus_httpserver.jar /jmx_reporter/jmx_prometheus_httpserver.jar
COPY ./config.yaml /jmx_reporter/config.yaml
COPY ./entrypoint.sh /apache-storm-2.4.0/entrypoint.sh
RUN ["chmod", "+x", "/apache-storm-2.4.0/entrypoint.sh"]

#ENV NIMBUS_SEED=nimbus KAFKA_SEED=kafka1:9092 KAFKA_INPUT_TOPIC=click KAFKA_UPDATE_TOPIC=update KAFKA_OUTPUT_TPOIC=output
ENV COMMAND="storm jar /apache-storm-2.4.0/bin/storm-topology.jar de.tum.in.msrg.storm.Submitter -nimbus $NIMBUS_SEED -kafka $KAFKA_SEED -input $KAFKA_INPUT_TOPIC -update $KAFKA_UPDATE_TOPIC -output $KAFKA_OUTPUT_TPOIC"
EXPOSE 12345 12346
WORKDIR /

ENTRYPOINT ["/apache-storm-2.4.0/entrypoint.sh"]
