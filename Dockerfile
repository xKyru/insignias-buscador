# ====== Build ======
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# ====== Runtime ======
FROM eclipse-temurin:17-jre
WORKDIR /app
# copia el jar (ajusta el nombre si tu artefacto no es *-SNAPSHOT.jar)
COPY --from=build /app/target/*.jar app.jar
# puerto interno (cada servicio respeta su propio application.yml)
EXPOSE 8080 8761 8083 8084
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
