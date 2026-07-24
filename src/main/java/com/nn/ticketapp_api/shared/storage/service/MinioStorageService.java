package com.nn.ticketapp_api.shared.storage.service;

import com.nn.ticketapp_api.shared.storage.config.MinioProperties;
import com.nn.ticketapp_api.shared.storage.exception.StorageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public String uploadFile(String objectKey, InputStream inputStream, String contentType, long size) {
        log.debug("Uploading file '{}' to bucket '{}'", objectKey, minioProperties.bucketName());
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucketName())
                            .object(objectKey)
                            .stream(inputStream, size, -1L)
                            .contentType(contentType)
                            .build()
            );
            log.info("File '{}' successfully uploaded.", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload file '{}' to MinIO", objectKey, e);
            throw new StorageException("Could not store file in object storage", e);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        log.debug("Deleting file '{}' from bucket '{}'", objectKey, minioProperties.bucketName());
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucketName())
                            .object(objectKey)
                            .build()
            );
            log.info("File '{}' successfully deleted.", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete file '{}' from MinIO", objectKey, e);
            throw new StorageException("Could not delete file in object storage", e);
        }
    }
}
