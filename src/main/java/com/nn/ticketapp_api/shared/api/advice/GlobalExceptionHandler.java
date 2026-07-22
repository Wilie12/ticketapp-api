package com.nn.ticketapp_api.shared.api.advice;

import com.nn.ticketapp_api.shared.api.response.ErrorResponse;
import com.nn.ticketapp_api.ticket.exception.InvalidStatusTransitionException;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import com.nn.ticketapp_api.ticket.exception.TicketNotFoundException;
import com.nn.ticketapp_api.ticket.exception.TicketOwnershipException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTicketNotFoundException(
            TicketNotFoundException e,
            HttpServletRequest request
    ) {
        log.warn("{}", e.getMessage());

        return new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error ->
                        String.format(
                                "Validation failed for field '%s': %s",
                                error.getField(),
                                error.getDefaultMessage()
                        )
                )
                .collect(Collectors.joining("; "));

        log.warn("Validation error on path: {}: {}", request.getRequestURI(), message);

        return new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler({TicketOwnershipException.class, AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbiddenExceptions(
            RuntimeException e,
            HttpServletRequest request
    ) {
        log.warn("{}", e.getMessage());

        return new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler({TicketClosedException.class, InvalidStatusTransitionException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflictExceptions(
            RuntimeException e,
            HttpServletRequest request
    ) {
        log.warn("State conflict error on path: {}: {}", request.getRequestURI(), e.getMessage());

        return new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI()
        );
    }
}