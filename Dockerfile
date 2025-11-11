# Stage 1: Build with Maven + JDK 21
FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /app

ARG GITHUB_USERNAME
ARG GITHUB_TOKEN

COPY pom.xml .
# Run 'mvn dependency:go-offline' to cache the dependencies
RUN mkdir -p /root/.m2 && \
    echo "<settings><servers><server><id>github</id><username>${GITHUB_USERNAME}</username><password>${GITHUB_TOKEN}</password></server></servers></settings>" > /root/.m2/settings.xml && \
    cat /root/.m2/settings.xml && \
    mvn -s /root/.m2/settings.xml dependency:go-offline

# Copy the rest of the source code after the dependencies are cached
COPY src ./src
RUN mvn clean package -DskipTests && cp target/*.jar target/app.jar

# Stage 2: Create the final image with JDK 21 that just runs the application
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app
COPY --from=builder /app/target/app.jar app.jar
# Expose the necessary port
EXPOSE 8080
# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--debug"]
