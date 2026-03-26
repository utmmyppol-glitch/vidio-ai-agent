# ===== Stage 1: Build Frontend =====
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ===== Stage 2: Build Backend =====
FROM gradle:8.8-jdk17 AS backend-build
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle/ gradle/
RUN gradle dependencies --no-daemon || true
COPY src/ src/
# Copy frontend build output to Spring Boot static resources
COPY --from=frontend-build /app/frontend/dist/ src/main/resources/static/
RUN gradle bootJar --no-daemon -x test

# ===== Stage 3: Runtime =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install FFmpeg for video generation
RUN apk add --no-cache ffmpeg fonts-noto-cjk || apk add --no-cache ffmpeg

# Create directories for generated content
RUN mkdir -p /app/generated-content/videos /app/generated-content/thumbnails

COPY --from=backend-build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
