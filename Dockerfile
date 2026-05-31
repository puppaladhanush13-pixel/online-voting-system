# ====== Builder stage ======
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ====== Runner stage ======
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/OnlineVotingSystem-0.0.1-SNAPSHOT.jar app.jar
# Persist uploaded candidate images outside the image
VOLUME ["/app/uploads"]
EXPOSE 8000
# DB connection can be overridden at runtime, e.g.:
#   docker run -e DB_HOST=mysql -e DB_PORT=3306 -e DB_NAME=Online_voting_system \
#              -e DB_USERNAME=root -e DB_PASSWORD=system@123 -p 8000:8000 <image>
ENTRYPOINT ["java", "-jar", "app.jar"]
