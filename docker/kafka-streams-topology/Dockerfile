FROM maven:3.6.3-openjdk-11-slim AS maven

COPY ./ /kafka-topology
COPY settings.xml $MAVEN_CONFIG/settings.xml

WORKDIR /kafka-topology

RUN mvn clean package

FROM openjdk:11-jdk

COPY --from=maven /kafka-topology/target/kafka-streams-topology*.jar /kafka-topology/kafka-streams-topology.jar
COPY --from=maven /kafka-topology/jmx_prometheus_httpserver*.jar /jmx-exporter/jmx_prometheus_httpserver.jar
COPY --from=maven /kafka-topology/config.yaml /jmx-exporter/config.yaml
COPY --from=maven /kafka-topology/entrypoint.sh /entrypoint.sh
RUN ["chmod", "+x", "./entrypoint.sh"]

#WORKDIR /jmx-exporter

#RUN nohup java -jar jmx_prometheus_httpserver.jar 12345 config.yaml &
EXPOSE 12345 12346

VOLUME /kafka-topology /tmp/kafka-streams
WORKDIR /
ENV APPID=kstreams-eventcount KAFKA_BOOTSTRAP=kafka:9092 INPUT_TOPIC=input OUTPUT_TOPIC=output PROCESSING_GUARANTEE=exactly_once_beta STANDBY=0

ENTRYPOINT ["./entrypoint.sh"]

#-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=52923