FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine

RUN apk upgrade --no-cache \
    && apk add --no-cache curl \
    && addgroup -S wms \
    && adduser -S wms -G wms

WORKDIR /app

COPY --from=build --chown=wms:wms /workspace/target/wms-backend-0.0.1-SNAPSHOT.jar app.jar

USER wms

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
