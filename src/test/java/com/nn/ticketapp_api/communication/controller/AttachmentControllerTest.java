package com.nn.ticketapp_api.communication.controller;

import com.nn.ticketapp_api.communication.api.advice.CommunicationExceptionHandler;
import com.nn.ticketapp_api.communication.api.response.AttachmentResponse;
import com.nn.ticketapp_api.communication.domain.Attachment;
import com.nn.ticketapp_api.communication.exception.AttachmentOwnershipException;
import com.nn.ticketapp_api.communication.exception.InvalidAttachmentException;
import com.nn.ticketapp_api.communication.service.AttachmentService;
import com.nn.ticketapp_api.shared.api.advice.GlobalExceptionHandler;
import com.nn.ticketapp_api.shared.config.WebMvcConfig;
import com.nn.ticketapp_api.shared.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttachmentController.class)
@Import({SecurityConfig.class, WebMvcConfig.class})
public class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AttachmentService attachmentService;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private Clock clock;

    @Test
    @DisplayName("Should successfully upload attachment and return 201 Created")
    void shouldUploadAttachmentSuccessfully() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "log.txt",
                "text/plain",
                "Error context".getBytes()
        );

        AttachmentResponse mockResponse = new AttachmentResponse(
                UUID.randomUUID(),
                "log.txt",
                "http://localhost:9000/bucket/tickets/key",
                Instant.now()
        );

        given(attachmentService.uploadAttachment(eq(ticketId), eq(authorId), any())).willReturn(mockResponse);

        // when
        mockMvc.perform(multipart("/api/v1/tickets/{ticketId}/attachments", ticketId)
                        .file(mockFile)
                        .with(jwt().jwt(builder -> builder.subject(authorId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("log.txt"))
                .andExpect(jsonPath("$.fileUrl")
                        .value("http://localhost:9000/bucket/tickets/key"));

        then(attachmentService).should().uploadAttachment(eq(ticketId), eq(authorId), any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when uploading invalid file")
    void shouldReturnBadRequestOnInvalidFile() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../../etc",
                "text/plain",
                new byte[0]
        );

        given(attachmentService.uploadAttachment(eq(ticketId), eq(authorId), any()))
                .willThrow(new InvalidAttachmentException("Filename contains invalid path sequence"));

        // when
        mockMvc.perform(multipart("/api/v1/tickets/{ticketId}/attachments", ticketId)
                        .file(maliciousFile)
                        .with(jwt().jwt(builder -> builder.subject(authorId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail")
                        .value("Filename contains invalid path sequence"));
    }

    @Test
    @DisplayName("Should successfully delete attachment and return 204 No Content")
    void shouldDeleteAttachmentSuccessfully() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        // when
        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/attachments/{attachmentId}", ticketId, attachmentId)
                        .with(jwt().jwt(builder -> builder.subject(requesterId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                // then
                .andExpect(status().isNoContent());

        then(attachmentService).should().deleteAttachment(ticketId, attachmentId, requesterId);
    }

    @Test
    @DisplayName("Should return 403 Forbidden when deleting attachment without ownership")
    void shouldReturnForbiddenWhenDeletingWithoutOwnership() throws Exception {
        // given
        UUID ticketId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID maliciousUserId = UUID.randomUUID();

        doThrow(new AttachmentOwnershipException(attachmentId, maliciousUserId))
                .when(attachmentService).deleteAttachment(ticketId, attachmentId, maliciousUserId);

        // when
        mockMvc.perform(delete("/api/v1/tickets/{ticketId}/attachments/{attachmentId}", ticketId, attachmentId)
                        .with(jwt().jwt(builder -> builder.subject(maliciousUserId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                // then
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value(
                        String.format(
                                "User %s does not have permission to modify attachment %s",
                                maliciousUserId,
                                attachmentId
                        )
                ));
    }
}