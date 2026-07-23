# ==========================================
# STAGE 1: BUILD THE APPLICATION
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Fetch all dependencies to cache them inside Docker layers
RUN mvn dependency:go-offline -B

# Copy full application source files
COPY src ./src

# Compile and package into a self-contained executable JAR
RUN mvn clean package -DskipTests -B

# ==========================================
# STAGE 2: RUNTIME ENGINE
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-privileged system user for secure least-privilege execution
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from Stage 1 builder
COPY --from=builder /app/target/platform-1.0.0.jar ./medsupply-platform.jar

# Expose standard default Spring Boot port
EXPOSE 8080

# Environment optimization variables for JVM inside container structures
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -Xms512m -Xmx1024m"

# Execute application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar medsupply-platform.jar"]
