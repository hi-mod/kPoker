# --- BUILD STAGE ---
FROM mingc/android-build-box AS build
WORKDIR /project

# 1. Copy Gradle configuration first to cache dependencies
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy all module build files
COPY composeApp/build.gradle.kts composeApp/
COPY kPoker/build.gradle.kts kPoker/
COPY server/build.gradle.kts server/
COPY shared/build.gradle.kts shared/

# 2. Download dependencies (This layer will be cached)
RUN chmod +x ./gradlew
RUN ./gradlew help --no-daemon

# 3. Copy source code and build
COPY . .
RUN chmod +x ./gradlew
# Using Production Distribution for deployment
RUN ./gradlew :composeApp:wasmJsBrowserDistribution :server:shadowJar --no-daemon

# --- RUNTIME STAGE ---
FROM eclipse-temurin:21-jre-alpine AS runtime
EXPOSE 8080
WORKDIR /app

# Copy the Production Wasm frontend files to the 'static' folder
COPY --from=build /project/composeApp/build/dist/wasmJs/productionExecutable /app/static

# Copy the Ktor Server Fat JAR
COPY --from=build /project/server/build/libs/*-all.jar /app/ktor-server.jar

# Run the server
ENTRYPOINT ["java", "-jar", "/app/ktor-server.jar"]