# ---------- build ----------
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace/app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test


# ---------- runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar /app/app.jar

ENV TZ=Asia/Seoul

ENTRYPOINT ["java", "-jar", "/app/app.jar"]