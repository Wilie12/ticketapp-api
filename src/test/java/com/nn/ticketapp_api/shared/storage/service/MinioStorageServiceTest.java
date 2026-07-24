package com.nn.ticketapp_api.shared.storage.service;

import com.nn.ticketapp_api.shared.storage.config.MinioProperties;
import com.nn.ticketapp_api.shared.storage.exception.StorageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;
    @Mock
    private MinioProperties minioProperties;
    @InjectMocks
    private MinioStorageService minioStorageService;
    @Captor
    private ArgumentCaptor<PutObjectArgs> putObjectArgCaptor;
    @Captor
    private ArgumentCaptor<RemoveObjectArgs> removeObjectArgCaptor;

    @Test
    @DisplayName("Should successfully upload file and return its object key")
    void shouldUploadFileSuccessfully() throws Exception {
        // given
        String objectKey = "tickets/INC0000001/file.txt";
        String bucketName = "test-bucket";
        String contentType = "text/plain";
        byte[] content = "Hello World".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        given(minioProperties.bucketName()).willReturn(bucketName);

        // when
        String resultKey = minioStorageService.uploadFile(objectKey, inputStream, contentType, content.length);

        // then
        assertThat(resultKey).isEqualTo(objectKey);

        then(minioClient).should().putObject(putObjectArgCaptor.capture());
        PutObjectArgs capturedArgs = putObjectArgCaptor.getValue();

        assertThat(capturedArgs.bucket()).isEqualTo(bucketName);
        assertThat(capturedArgs.object()).isEqualTo(objectKey);
        assertThat(capturedArgs.contentType().toString()).isEqualTo(contentType);
    }

    @Test
    @DisplayName("Should wrap MinIO exceptions in domain StorageException on upload feature")
    void shouldThrowStorageExceptionOnUploadFailure() throws Exception {
        // given
        String objectKey = "tickets/error.txt";
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        given(minioProperties.bucketName()).willReturn("test-bucket");
        given(minioClient.putObject(any(PutObjectArgs.class)))
                .willThrow(new RuntimeException("MinIO test exception"));

        // when
        Throwable thrown = catchThrowable(() ->
                minioStorageService.uploadFile(objectKey, inputStream, "text/plain", 0)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Could not store file in object storage");
    }

    @Test
    @DisplayName("Should successfully execute delete operation on object storage")
    void shouldDeleteFileSuccessfully() throws Exception {
        // given
        String objectKey = "tickets/INC0000001/file-to-delete.txt";
        String bucketName = "test-bucket";

        given(minioProperties.bucketName()).willReturn(bucketName);

        // when
        minioStorageService.deleteFile(objectKey);

        // then
        then(minioClient).should().removeObject(removeObjectArgCaptor.capture());
        RemoveObjectArgs capturedArgs = removeObjectArgCaptor.getValue();

        assertThat(capturedArgs.bucket()).isEqualTo(bucketName);
        assertThat(capturedArgs.object()).isEqualTo(objectKey);
    }

    @Test
    @DisplayName("Should wrap MinIO exceptions in domain StorageException on delete failure")
    void shouldThrowStorageExceptionOnDeleteFailure() throws Exception {
        // given
        String objectKey = "tickets/corrupted.txt";
        String bucketName = "test-bucket";

        given(minioProperties.bucketName()).willReturn(bucketName);
        doThrow(new RuntimeException("MinIO test exception"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        // when
        Throwable thrown = catchThrowable(() -> minioStorageService.deleteFile(objectKey));

        // then
        assertThat(thrown)
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Could not delete file in object storage");
    }
}
