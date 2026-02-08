package com.sebastianhauss.videoplatform.storage.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    private String url;
    private int port;
    private String user;
    private String password;
    private String bucket;
}
