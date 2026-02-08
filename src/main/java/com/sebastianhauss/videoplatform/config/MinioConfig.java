package com.sebastianhauss.videoplatform.config;

import com.sebastianhauss.videoplatform.storage.minio.MinioProperties;
import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(
                        minioProperties.getUrl(),
                        minioProperties.getPort(),
                        minioProperties.getUrl().contains("https"))
                .credentials(
                        minioProperties.getUser(),
                        minioProperties.getPassword())
                .build();
    }
}