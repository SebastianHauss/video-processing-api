package com.sebastianhauss.videoplatform.storage;

import com.sebastianhauss.videoplatform.dto.storage.StoredObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;

public interface StorageService {
    StoredObject upload(MultipartFile file, String objectKey, String contentType);
    StoredObject uploadFromFile(File file, String objectKey, String contentType);
    InputStream download(StoredObject stored);

    /**
     * Download a contiguous byte range of an object, starting at {@code offset}
     * for {@code length} bytes. Backends should fetch only the requested slice
     * (e.g. MinIO {@code GetObjectArgs.offset/length}) rather than the whole
     * object, so range streaming stays cheap.
     */
    InputStream downloadRange(StoredObject stored, long offset, long length);

    void delete(String objectKey);
}