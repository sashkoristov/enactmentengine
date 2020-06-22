FROM adoptopenjdk/openjdk14:ubi
WORKDIR /
ADD build/libs/enactment-engine-all.jar service.jar
COPY Database /Database
EXPOSE 9898
ENTRYPOINT ["java", "-jar", "service.jar"]
CMD []