package com.nn.ticketapp_api.communication.api.advice;

import com.nn.ticketapp_api.communication.controller.AttachmentController;
import com.nn.ticketapp_api.communication.controller.CommunicationController;
import com.nn.ticketapp_api.communication.exception.AttachmentNotFoundException;
import com.nn.ticketapp_api.communication.exception.AttachmentOwnershipException;
import com.nn.ticketapp_api.communication.exception.InvalidAttachmentException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@RestControllerAdvice(assignableTypes = {AttachmentController.class, CommunicationController.class})
@RequiredArgsConstructor
public class CommunicationExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(AttachmentNotFoundException.class)
    public ProblemDetail handleNotFound(AttachmentNotFoundException e, HttpServletRequest request) {
        log.warn("Attachment resource not found on path {}: {}", request.getRequestURI(), e.getMessage());

        return createProblemDetail(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());
    }

    @ExceptionHandler(AttachmentOwnershipException.class)
    public ProblemDetail handleForbidden(AttachmentOwnershipException e, HttpServletRequest request) {
        log.warn("Attachment ownership violation on path {}: {}", request.getRequestURI(), e.getMessage());

        return createProblemDetail(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage());
    }

    @ExceptionHandler(InvalidAttachmentException.class)
    public ProblemDetail handleBadRequest(InvalidAttachmentException e, HttpServletRequest request) {
        log.warn("Invalid attachment request on path {}: {}", request.getRequestURI(), e.getMessage());

        return createProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now(clock));

        return problemDetail;
    }
}
