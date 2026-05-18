FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
# Kopiujemy pom.xml i pobieramy zależności, aby Docker je zapisał w cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kopiujemy kod źródłowy i budujemy
COPY src src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]