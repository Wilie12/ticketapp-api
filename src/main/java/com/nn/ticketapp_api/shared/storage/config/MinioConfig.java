package com.nn.ticketapp_api.shared.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.url())
                .credentials(minioProperties.accessKey(), minioProperties.secretKey())
                .build();

        initializeBucket(minioClient);

        return minioClient;
    }

    private void initializeBucket(MinioClient minioClient) throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.bucketName()).build()
        );

        if (!bucketExists) {
            log.info("Bucket {} does not exist. Creating it now.", minioProperties.bucketName());
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(minioProperties.bucketName()).build()
            );
            log.info("Bucket {} created successfully", minioProperties.bucketName());
        } else {
            log.info("Bucket {} already exists", minioProperties.bucketName());
        }
    }
}
