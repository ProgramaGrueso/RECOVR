# syntax=docker/dockerfile:1
# Imagen del backend RECOVR en dos etapas: compila con Maven y ejecuta solo el JAR sobre un JRE 21 liviano.

# --- Etapa 1: compilación ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Etapa 2: ejecución ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# Usuario sin privilegios
RUN useradd --system --uid 10001 recovr
COPY --from=build /app/target/backend-*.jar /app/recovr.jar
USER recovr
ENV TZ=America/Lima \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8095
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8095/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/recovr.jar"]
