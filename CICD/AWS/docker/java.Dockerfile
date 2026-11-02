FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /src
COPY Middleware /src
RUN mvn -B -f /src/pom.xml -pl "${SERVICE}" -am -DskipTests package \
    && find "/src/${SERVICE}/target" -maxdepth 1 -type f -name "${SERVICE}-*.jar" ! -name "*.original" -exec cp {} /tmp/app.jar \;
FROM eclipse-temurin:21-jre-jammy
ARG PORT=8080
COPY --from=build /tmp/app.jar /app/app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
