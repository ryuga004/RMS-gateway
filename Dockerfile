# -------- BUILD STAGE --------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

# JVM tuning for Render's low-memory containers (512MB–1GB free tier)
ENV JAVA_TOOL_OPTIONS="\
-XX:+UseContainerSupport \
-XX:+UseSerialGC \
-XX:MaxRAMPercentage=70.0 \
-XX:InitialRAMPercentage=20.0 \
-XX:MaxMetaspaceSize=128m \
-XX:+ExitOnOutOfMemoryError"

# Render injects $PORT at runtime — shell form expands it
# All other config (Postgres, Redis, JWT secret, Eureka, etc.) comes from Render env vars
EXPOSE 8082

USER spring

CMD ["sh", "-c", "java $JAVA_TOOL_OPTIONS -jar /app/app.jar --server.port=${PORT:-8082} --spring.profiles.active=prod"]
