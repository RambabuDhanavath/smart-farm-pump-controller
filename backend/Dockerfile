# Backend image for the Smart Farm Pump Controller (Spring Boot)
#
# Build the jar first:  mvn clean package
# Then build this image via docker-compose at the repo root.

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# The jar built by `mvn clean package` (rename if your artifact differs)
COPY target/pump-controller-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
