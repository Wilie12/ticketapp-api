package com.nn.ticketapp_api.agent.api.advice;

import com.nn.ticketapp_api.agent.controller.AgentProfileController;
import com.nn.ticketapp_api.agent.exception.AgentNotFoundException;
import com.nn.ticketapp_api.agent.exception.AgentProfileAlreadyExistsException;
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
@RestControllerAdvice(assignableTypes = AgentProfileController.class)
@RequiredArgsConstructor
public class AgentExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(AgentNotFoundException.class)
    public ProblemDetail handleNotFound(AgentNotFoundException e, HttpServletRequest request) {
        log.warn("Agent profile not found on path {}: {}", request.getRequestURI(), e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Not Found");
        problemDetail.setProperty("timestamp", Instant.now(clock));

        return problemDetail;
    }

    @ExceptionHandler(AgentProfileAlreadyExistsException.class)
    public ProblemDetail handleConflict(AgentProfileAlreadyExistsException e, HttpServletRequest request) {
        log.warn("Agent profile conflict on path {}: {}", request.getRequestURI(), e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problemDetail.setTitle("Conflict");
        problemDetail.setProperty("timestamp", Instant.now(clock));

        return problemDetail;
    }
}
