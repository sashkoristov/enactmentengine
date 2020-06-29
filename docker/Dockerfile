FROM openjdk:14
WORKDIR /
ADD Database/enactment-engine-all.jar ee-service.jar
COPY Database /Database
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "ee-service.jar"]
CMD []