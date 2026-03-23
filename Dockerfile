FROM eclipse-temurin:21-jdk-alpine AS runtime

WORKDIR /app

ARG JAR_FILE=target/wristband-printer-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENV PRINTER_HOST=printer
ENV PRINTER_PORT=9100

ENTRYPOINT ["java","-jar","/app/app.jar"]

