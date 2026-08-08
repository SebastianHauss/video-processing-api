package com.sebastianhauss.videoplatform.storage.minio;

import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.exception.StorageException;
import com.sebastianhauss.videoplatform.storage.StorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    /**
     * Create the configured bucket if it does not exist yet, so a fresh MinIO
     * instance doesn't fail the first upload with "bucket does not exist".
     */
    @PostConstruct
    void ensureBucketExists() {
        String bucket = minioProperties.getBucket();
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (exists) {
                log.info("MinIO bucket '{}' already exists", bucket);
                return;
            }
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket '{}'", bucket);
        } catch (Exception e) {
            // Best-effort: don't couple app startup to MinIO being reachable (it
            // may still be coming up). If the bucket is genuinely missing, the
            // first upload will surface it.
            log.warn("Could not ensure MinIO bucket '{}' exists at startup: {}", bucket, e.getMessage());
        }
    }

    @Override
    public StoredObject upload(MultipartFile file, String objectKey, String contentType) {
        try (InputStream is = file.getInputStream()) {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(is, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("File uploaded to MinIO: {}", objectKey);
            return new StoredObject(minioProperties.getBucket(), objectKey);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: {}", objectKey, e);
            throw new StorageException("Could not upload file", e);
        }
    }

    @Override
    public StoredObject uploadFromFile(File file, String objectKey, String contentType) {
        try (InputStream is = new FileInputStream(file)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .stream(is, file.length(), -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("File uploaded to MinIO: {}", objectKey);
            return new StoredObject(minioProperties.getBucket(), objectKey);
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: {}", objectKey, e);
            throw new StorageException("Could not upload file", e);
        }
    }

    @Override
    public InputStream download(StoredObject stored) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(stored.objectKey())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", stored.objectKey(), e);
            throw new StorageException("Could not download file", e);
        }
    }

    @Override
    public InputStream downloadRange(StoredObject stored, long offset, long length) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(stored.objectKey())
                            .offset(offset)
                            .length(length)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download byte range [{}, +{}) from MinIO: {}",
                    offset, length, stored.objectKey(), e);
            throw new StorageException("Could not download file range", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .build()
            );

            log.info("Deleted file from MinIO: {}", objectKey);

        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", objectKey, e);
            throw new StorageException("Could not delete file", e);
        }
    }
}
