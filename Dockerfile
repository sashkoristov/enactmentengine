FROM openjdk:11.0.7-jre-slim
WORKDIR /
ADD target/enactment-engine-0.0.1-SNAPSHOT-jar-with-dependencies.jar service.jar
COPY Database /Database
EXPOSE 9898
ENTRYPOINT ["java", "-jar", "service.jar"]
CMD []