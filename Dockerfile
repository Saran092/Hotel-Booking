FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/hotel-booking-1.0.0.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx256m"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
