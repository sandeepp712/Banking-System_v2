# ==========================================
# STAGE 1: The Builder (Kitchen)
# ==========================================
# We need a full JDK and Maven to compile the code.
# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder


# Set the working directory inside the container
WORKDIR /app

#1. Copy only the pom.xml first
# Docker cache layers. If pom.xml doesn't change, Maven dependencies
# are download only once, making future builds 10x faster
COPY pom.xml .


#2. Download all dependencies(offline mode)
# This caches the dependencies so they don't need to be redownloaded if code changes
RUN mvn dependency:go-offline -B

#3. Copy the rest of the source code
# source code changes frequently, so we copy it at the end to avoid breaking the cache.
COPY src ./src


#4. Build the Jar(skip tests here, we run the tests separately in CI)
# In production builds(local development), we skip tests to save time, but we should always runt tests in a separate CI step before building the image.
RUN mvn clean package -DskipTests




# ==========================================
# STAGE 2: The Runtime (Serving the meal)
# ==========================================
# We need tiny JRE(java runtime environment) because we only need to run the code.
# Alpine Linux is super small, drastically reducing the final image size
FROM eclipse-temurin:21-jre-alpine

# Set the working directory for the runtime
WORKDIR /app

#1. Create a non-root user
#Security BEST PRACTICE. Running applications as root inside a container
# If a hacker compromises the app, they get the root access.
# We create an 'appuser' with limited permission
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser


#2 Copy the JAR from the Builder stage
# we only copy the final JAR, not the source code or Maven
COPY --from=builder /app/target/*.jar app.jar

#3. Expose port 8080
# This is the port our spring boot app listens on
EXPOSE 8080

#4. Define the startup command
# Tells the container to run the JAR. Flyway will run automatically inside
# the jar because our application.properties has spring.flyway.enabled=true
ENTRYPOINT ["java","-jar","app.jar"]