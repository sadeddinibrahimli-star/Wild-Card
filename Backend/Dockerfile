# stage 1: build the jar with Maven
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# copy only pom.xml first -> dependencies get cached in a layer,
# so changing the source does not rebuild them (VERY important).
COPY pom.xml .
RUN mvn -B dependency:go-offline

# then the source code
COPY src ./src
RUN mvn -B clean package -DskipTests


# stage 2: runtime only. Maven (~800MB) is NOT included.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# do not run as root
RUN groupadd --system wild && useradd --system --gid wild --create-home wild
# create the directories FIRST, then chown, then switch to USER.
# (mkdir AFTER USER gives "Permission denied")
RUN mkdir -p /app/uploads && chown -R wild:wild /app
USER wild

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/v3/api-docs > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]