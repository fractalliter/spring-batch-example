FROM maven:3.9.1-eclipse-temurin-17-alpine AS build
WORKDIR /winners
COPY --chown=maven:maven . /winners
RUN mvn package

FROM openjdk:17-alpine
RUN mkdir "app"
COPY --from=build /winners/target/*.jar /app/
WORKDIR /app
ENTRYPOINT ["java","-jar","winners-0.0.1-SNAPSHOT.jar"]