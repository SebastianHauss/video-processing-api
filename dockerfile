# --- ffmpeg: pinned static build, independent of the base image's package repo ---
# Gives a known, reproducible ffmpeg version. Verify/bump the tag at:
# https://hub.docker.com/r/mwader/static-ffmpeg/tags
FROM mwader/static-ffmpeg:7.1 AS ffmpeg

# --- build stage: compile the jar inside the image (no host build required) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
# dependency layer first so it caches unless pom.xml changes
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
# then the sources
COPY src ./src
RUN ./mvnw -q clean package -DskipTests

# --- runtime image ---
FROM eclipse-temurin:21-jre
COPY --from=ffmpeg /ffmpeg /usr/local/bin/ffmpeg
COPY --from=ffmpeg /ffprobe /usr/local/bin/ffprobe

WORKDIR /app
COPY --from=build /build/target/video-platform-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
