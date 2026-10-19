# ---- Stage 1: Build Angular frontend ----
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
# Angular outputs to ../src/main/resources/static (relative to frontend/)
RUN npm run build

# ---- Stage 2: Build Spring Boot jar (includes built frontend) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
# Replace empty src/main/resources/static with the Angular build output
RUN rm -rf src/main/resources/static
COPY --from=frontend /app/src/main/resources/static ./src/main/resources/static
RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

# ---- Stage 3: Runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/hotel-booking-1.0.0.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx256m"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
