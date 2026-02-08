package com.sebastianhauss.videoplatform.storage;

import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;

public interface StorageService {
    StoredObject upload(MultipartFile file, String objectKey, String contentType);
    StoredObject uploadFromFile(File file, String objectKey, String contentType);
    InputStream download(StoredObject stored);
    void delete(String objectKey);
}