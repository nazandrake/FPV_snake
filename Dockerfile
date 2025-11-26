# Stage 1: Build the frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Stage 2: Build the backend with bundled frontend
FROM gradle:8.8-jdk17 AS backend-builder
WORKDIR /app/backend
# Copy the built frontend assets into the backend's resources
COPY --from=frontend-builder /app/frontend/dist /app/backend/src/main/resources/static
COPY backend/ /app/backend/
# Build the backend. The frontend assets will be included in the JAR.
RUN gradle build -x test

# Stage 3: Create the final, self-contained image
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
# Copy only the final, runnable "fat" JAR file
COPY --from=backend-builder /app/backend/build/libs/backend-all.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]