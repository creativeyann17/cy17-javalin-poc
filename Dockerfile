FROM maven:3.9.4-amazoncorretto-21 as build-api
WORKDIR /tmp/api
COPY . .
RUN mvn clean install -DskipTests

FROM amazoncorretto:21-alpine as build-jre
WORKDIR /tmp/jre
# required for strip-debug to work
RUN apk add --no-cache binutils
# Build small JRE image
# jdeps --multi-release 21 --recursive -cp "$(find . -name "*.jar" -printf "%h/*\n" | sort -u | paste -sd ":")" --ignore-missing-deps --print-module-deps ./  | tail -1
RUN jlink \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output slim

FROM alpine:latest
WORKDIR /app
ENV JAVA_HOME=/jre
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XshowSettings:vm -XX:+PrintCommandLineFlags"
ENV PATH="$PATH:$JAVA_HOME/bin"
RUN apk update && apk add ca-certificates openssl
COPY --from=build-jre /tmp/jre/slim $JAVA_HOME
COPY --from=build-api /tmp/api/target/cy17-javalin-poc-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT java $JAVA_OPTS -jar app.jar
