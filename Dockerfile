FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN ./mvnw -q -B clean package -DskipTests 2>/dev/null || \
    (apk add --no-cache maven && mvn -q -B clean package -DskipTests)

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Required: prevents ImageIO / Graphics2D from hanging on a headless host
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

ENV PRINTER_HOST=printer
ENV PRINTER_PORT=9100

COPY --from=build /build/target/wristband-printer-service-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
