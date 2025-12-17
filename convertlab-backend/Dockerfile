# ---------- Build stage ----------
FROM maven:3.9-amazoncorretto-25 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Runtime stage ----------
FROM amazoncorretto:25

WORKDIR /app

COPY --from=builder /app/target/convertlab-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
