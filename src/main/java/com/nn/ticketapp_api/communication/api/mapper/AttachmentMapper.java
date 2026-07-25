package com.nn.ticketapp_api.communication.api.mapper;

import com.nn.ticketapp_api.communication.api.response.AttachmentResponse;
import com.nn.ticketapp_api.communication.domain.Attachment;
import com.nn.ticketapp_api.shared.storage.config.MinioProperties;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class AttachmentMapper {

    @Autowired
    protected MinioProperties minioProperties;

    @Mapping(target = "filename", source = "originalFilename")
    @Mapping(target = "fileUrl", expression = "java(buildFileUrl(attachment.getObjectKey()))")
    public abstract AttachmentResponse toResponse(Attachment attachment);

    protected String buildFileUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }

        return String.format("%s/%s/%s", minioProperties.url(), minioProperties.bucketName(), objectKey);
    }
}
