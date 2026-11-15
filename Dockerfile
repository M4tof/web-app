# Stage 1: Build the .jar using Maven on Alpine Linux
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy your source code and pom.xml
COPY pom.xml .
COPY src ./src

# Compile and package everything (HTML, CSS, Java) into a JAR
RUN mvn clean package -DskipTests

# Stage 2: Create the minimal production runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the generated JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# Launch the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]