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

# Copia primero el pom para cachear dependencias (capa reutilizable).
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copia el codigo fuente y empaqueta (sin tests para un build mas rapido).
COPY src ./src
RUN mvn -B -DskipTests clean package

# --- Etapa 2: runtime ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia el JAR generado desde la etapa de build.
COPY --from=build /app/target/golia-backend-1.0.0.jar app.jar

# Arranca la aplicacion. Spring Boot leera PORT y las variables DB_* del entorno.
ENTRYPOINT ["java", "-jar", "app.jar"]
