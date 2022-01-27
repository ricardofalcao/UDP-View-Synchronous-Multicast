FROM openjdk:11

VOLUME ["/data"]

RUN apt-get update && apt-get install -y iproute2
ENTRYPOINT ["java", "-jar", "/data/App.jar"]