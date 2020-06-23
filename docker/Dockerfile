FROM openjdk:14
WORKDIR /
ADD enactment-engine-all.jar service.jar
COPY Database /Database
EXPOSE 9898
ENTRYPOINT ["java", "-jar", "service.jar"]
CMD []