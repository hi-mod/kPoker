FROM gradle:8-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Wasm files are pre-copied to server/src/main/resources/static by deploy script
RUN gradle :server:shadowJar --no-daemon

FROM eclipse-temurin:21-jre AS runtime
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/server/build/libs/*-all.jar /app/ktor-server.jar
ENTRYPOINT ["java","-jar","/app/ktor-server.jar"]
