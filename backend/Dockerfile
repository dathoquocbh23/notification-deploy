# ---- Build stage: biên dịch Spring Boot bằng Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage: chỉ cần JRE để chạy jar ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Railway cấp cổng qua biến PORT; Spring đọc qua server.port=${PORT}
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
