# 1-ci merhele: Maven ile jar yigilir
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Once sadece pom.xml kopyalanir -> dependency ler qatda cache olunur,
# kaynak kod deyisende yeniden yigilmir (COK vacib).
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Sonra kaynak kod
COPY src ./src
RUN mvn -B clean package -DskipTests


# 2-ci merhele: yalnizca runtime. Maven (~800MB) iceride OLmur.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# root olaraq isletmirik
RUN groupadd --system wild && useradd --system --gid wild --create-home wild
# Qovluqlar YARADILIR, sonra sahib deyilir, sonra USER'e keclidilir.
# (USER-den SONRA mkdir etsek "Permission denied" aliriq)
RUN mkdir -p /app/uploads && chown -R wild:wild /app
USER wild

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/v3/api-docs > /dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]