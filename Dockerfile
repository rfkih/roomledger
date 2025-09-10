FROM gradle:8.7.0-jdk21-alpine AS build
WORKDIR /application
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle bootJar -x test && \
    JAR="$(ls build/libs | grep -E '\.jar$' | grep -v 'plain' | head -n1)" && \
    cp "build/libs/${JAR}" /application/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /application/app.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
