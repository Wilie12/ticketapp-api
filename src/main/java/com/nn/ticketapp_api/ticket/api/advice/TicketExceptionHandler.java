package com.nn.ticketapp_api.ticket.api.advice;

import com.nn.ticketapp_api.ticket.controller.AdminController;
import com.nn.ticketapp_api.ticket.controller.AgentController;
import com.nn.ticketapp_api.ticket.controller.TicketController;
import com.nn.ticketapp_api.ticket.exception.InvalidStatusTransitionException;
import com.nn.ticketapp_api.ticket.exception.TicketClosedException;
import com.nn.ticketapp_api.ticket.exception.TicketNotFoundException;
import com.nn.ticketapp_api.ticket.exception.TicketOwnershipException;
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
@RestControllerAdvice(assignableTypes = {TicketController.class, AgentController.class, AdminController.class})
@RequiredArgsConstructor
public class TicketExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(TicketNotFoundException.class)
    public ProblemDetail handleNotFound(TicketNotFoundException e, HttpServletRequest request) {
        log.warn("Ticket resource not found on path {}: {}", request.getRequestURI(), e.getMessage());

        return createProblemDetail(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());
    }

    @ExceptionHandler(TicketOwnershipException.class)
    public ProblemDetail handleForbidden(TicketOwnershipException e, HttpServletRequest request) {
        log.warn("Ticket ownership violation on path {}: {}", request.getRequestURI(), e.getMessage());

        return createProblemDetail(HttpStatus.FORBIDDEN, "Forbidden", e.getMessage());
    }

    @ExceptionHandler({TicketClosedException.class, InvalidStatusTransitionException.class})
    public ProblemDetail handleConflict(RuntimeException e, HttpServletRequest request) {
        log.warn("Ticket state conflict on path {}: {}", request.getRequestURI(), e.getMessage());

        return createProblemDetail(HttpStatus.CONFLICT, "Conflict", e.getMessage());
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now(clock));

        return problemDetail;
    }
}
