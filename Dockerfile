FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /winners
COPY --chown=maven:maven . /winners
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre
RUN mkdir "app"
COPY --from=build /winners/target/*.jar /app/
WORKDIR /app
ENTRYPOINT ["java","-jar","winners-0.0.1-SNAPSHOT.jar"]
