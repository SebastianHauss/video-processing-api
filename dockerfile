FROM eclipse-temurin:21-jdk

# FFmpeg installieren
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# Verify installation
RUN ffmpeg -version