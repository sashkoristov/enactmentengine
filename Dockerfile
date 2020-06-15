FROM java:8
WORKDIR /
ADD out/artifacts/enactment_engine_jar/enactment-engine.jar service.jar
EXPOSE 9898
ENTRYPOINT ["java", "-jar", "service.jar"]
CMD []