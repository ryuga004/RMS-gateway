# -------- BUILD STAGE --------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:17-jre-alpine

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy jar (avoid wildcard if possible for caching)
COPY --from=build /build/target/*.jar app.jar

# JVM tuning for low memory containers
ENV JAVA_TOOL_OPTIONS="\
-XX:+UseContainerSupport \
-XX:+UseSerialGC \
-XX:MaxRAMPercentage=70.0 \
-XX:InitialRAMPercentage=20.0 \
-XX:MaxMetaspaceSize=128m \
-XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

USER spring

ENTRYPOINT ["java","-jar","/app/app.jar"]
