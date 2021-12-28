FROM maven AS build
COPY src /PumbaSrvrClient/src
COPY pom.xml /PumbaSrvrClient/pom.xml
RUN mvn -f /PumbaSrvrClient/pom.xml clean compile assembly:single

FROM openjdk:8-jdk
COPY --from=build /PumbaSrvrClient/target/pumbasrvr-client*.jar /PumbaSrvrClient/pumbasrvrclient.jar
ENV PUMBA_SRVR_CLIENT_RESOURCE_DIR="/PumbaSrvrClient/"
COPY src/main/resources/experiment.yaml /PumbaSrvrClient/experiment.yaml
ENTRYPOINT ["java", "-jar", "/PumbaSrvrClient/pumbasrvrclient.jar"]