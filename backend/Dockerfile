# ==========================================
# STAGE 1: The Builder (Kitchen)
# ==========================================
# We need a full JDK and Maven to compile the code.
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder


# Set the working directory inside the container
WORKDIR /app

#1. Copy only the pom.xml first
# Docker cache layers. If pom.xml doesn't change, Maven dependencies
# are download only once, making future builds 10x faster
COPY mvnw pom.xml ./
COPY .mvn .mvn


#2. Download all dependencies(offline mode)
# This caches the dependencies so they don't need to be redownloaded if code changes
#RUN mvn dependency:go-offline -B


#3. Copy the rest of the source code
# source code changes frequently, so we copy it at the end to avoid breaking the cache.
COPY src src




#4. Build the Jar(skip tests here, we run the tests separately in CI)
# In production builds(local development), we skip tests to save time, but we should always runt tests in a separate CI step before building the image.
# Build the application
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests




# ==========================================
# STAGE 2: The Runtime (Serving the meal)
# ==========================================
# We need tiny JRE(java runtime environment) because we only need to run the code.
# Alpine Linux is super small, drastically reducing the final image size
FROM eclipse-temurin:21-jre-alpine


#Install curl for health checks
RUN apk add --no-cache curl

# Set the working directory for the runtime
WORKDIR /app

#1. Create a non-root user
#Security BEST PRACTICE. Running applications as root inside a container
# If a hacker compromises the app, they get the root access.
# We create an 'appuser' with limited permission

#RUN addgroup -S appgroup && adduser -S appuser -G appgroup
#USER appuser
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring


#2 Copy the JAR from the Builder stage
# we only copy the final JAR, not the source code or Maven
COPY --from=builder /app/target/*.jar app.jar

#Change ownership
RUN chown spring:spring app.jar

USER spring

#3. Expose port 8080
# This is the port our spring boot app listens on
EXPOSE 8080


#Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
CMD curl -f http://localhost:8080/actuator/health || exit 1


#4. Define the startup command
# Tells the container to run the JAR. Flyway will run automatically inside
# the jar because our application.properties has spring.flyway.enabled=true
ENTRYPOINT ["java","-jar","app.jar"]