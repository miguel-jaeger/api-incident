FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app/app
RUN mkdir -p /app/app/data

# Copiar todo el código fuente del proyecto
COPY app ./ 

# Construir proyecto
RUN mvn -B -DskipTests=true clean package

FROM eclipse-temurin:21-jre
WORKDIR /app/app
RUN mkdir -p /app/autenticar/db

# Copiar el archivo jar compilado
COPY --from=build /app/app/target/*.jar app.jar

# Copiar archivo de base de datos SQLite al contenedor
COPY app/data/incident.db /app/app/data/incident.db

CMD ["sh","-c","mkdir -p /app/app/data && java -Dserver.port=${PORT:-4002} -jar /app/app/app.jar"]
