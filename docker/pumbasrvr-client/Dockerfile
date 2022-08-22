FROM maven AS build
COPY src /PumbaSrvrClient/src
COPY pom.xml /PumbaSrvrClient/pom.xml
RUN mvn -f /PumbaSrvrClient/pom.xml clean compile assembly:single

FROM openjdk:11-jdk-slim
COPY --from=build /PumbaSrvrClient/target/pumbasrvr-client*.jar /PumbaSrvrClient/pumbasrvrclient.jar
#ENV PUMBA_SRVR_CLIENT_RESOURCE_DIR="/PumbaSrvrClient/"
#COPY src/main/resources/experiment.yaml /PumbaSrvrClient/experiment.yaml
WORKDIR /PumbaSrvrClient
VOLUME /PumbaSrvrClient
ENTRYPOINT ["java", "-cp", "*:.", "de.tum.msrg.PumbaSrvrClient"]