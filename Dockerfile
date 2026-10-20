# Dockerfile único para todos los microservicios.
# Uso: docker build --build-arg MODULE=order-service -t order-service .

# ---------- Etapa 1: compilación ----------
FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# La caché de ~/.m2 se reutiliza entre builds (BuildKit)
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl ${MODULE} -am -DskipTests package

# ---------- Etapa 2: imagen final, solo JRE ----------
FROM eclipse-temurin:21-jre
ARG MODULE
RUN useradd --system --uid 1001 spring
WORKDIR /app
COPY --from=build /src/${MODULE}/target/app.jar app.jar
USER spring
EXPOSE 8080
# MaxRAMPercentage: la JVM respeta el límite de memoria del contenedor (0.5 Gi en Container Apps)
# SerialGC: el recolector más liviano para contenedores con 1 vCPU o menos
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]
