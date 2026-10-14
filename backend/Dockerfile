# ======================================================================
# Dockerfile para el backend GolIA (Spring Boot + MySQL) en Render.
#
# Build multi-stage:
#   1) Etapa "build": compila el proyecto con Maven y genera el JAR.
#   2) Etapa final: imagen ligera solo con el JRE y el JAR ejecutable.
#
# Render construye esta imagen automaticamente al detectar el Dockerfile.
# El puerto lo define Render via la variable de entorno PORT (ya soportada
# por application.properties: server.port=${PORT:8080}).
# ======================================================================

# --- Etapa 1: compilacion ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia el pom y el codigo fuente, luego empaqueta (sin tests).
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

# --- Etapa 2: runtime ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia el JAR generado desde la etapa de build (comodin: evita depender
# del nombre/version exactos del artefacto).
COPY --from=build /app/target/*.jar app.jar

# Arranca la aplicacion. Spring Boot leera PORT y las variables DB_* del entorno.
ENTRYPOINT ["java", "-jar", "app.jar"]
