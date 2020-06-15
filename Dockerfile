FROM java:8
WORKDIR /
ADD target/enactment-engine-0.0.1-SNAPSHOT-jar-with-dependencies.jar service.jar
COPY Database /Database
EXPOSE 9898
ENTRYPOINT ["java", "-jar", "service.jar"]
CMD []