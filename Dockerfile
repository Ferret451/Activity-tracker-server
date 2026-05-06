# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts /app/
COPY gradle /app/gradle
RUN chmod +x /app/gradlew

COPY src /app/src
RUN /app/gradlew --no-daemon bootJar

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/*-SNAPSHOT.jar /app/app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

