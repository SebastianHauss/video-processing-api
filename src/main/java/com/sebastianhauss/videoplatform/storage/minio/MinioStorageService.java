package com.sebastianhauss.videoplatform.storage.minio;

import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import com.sebastianhauss.videoplatform.exception.StorageException;
import com.sebastianhauss.videoplatform.storage.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
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
