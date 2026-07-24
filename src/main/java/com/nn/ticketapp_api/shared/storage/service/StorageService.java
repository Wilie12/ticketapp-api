package com.nn.ticketapp_api.shared.storage.service;

import java.io.InputStream;

public interface StorageService {
    String uploadFile(String objectKey, InputStream inputStream, String contentType, long size);
    void deleteFile(String objectKey);
}
