# ===================================================
# Stage 1: Build the Spring Boot application with Maven
# ===================================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

# Copy Maven wrapper configuration and descriptor
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Grant execution rights for the Maven wrapper
RUN chmod +x ./mvnw

# Pre-fetch project dependencies to leverage Docker layer caching
RUN ./mvnw dependency:go-offline -B || true

# Copy project source code
COPY src ./src

# Package the executable Spring Boot JAR (skip tests during container build)
RUN ./mvnw clean package -DskipTests

# ===================================================
# Stage 2: Minimal Java 25 runtime environment
# ===================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create a dedicated non-root user and group for security
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy executable JAR artifact from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Expose the configured application port
EXPOSE 8081

# Launch the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
