# Backend RECOVR: Spring Boot (Java 21). Se compila dentro de Docker; no hace falta Maven ni JDK en tu equipo.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/*[!l].jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home recovr
COPY --from=build /app/app.jar app.jar
USER recovr
EXPOSE 8095
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
