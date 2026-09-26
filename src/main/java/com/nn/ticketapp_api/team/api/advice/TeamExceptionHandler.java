package com.nn.ticketapp_api.team.api.advice;

import com.nn.ticketapp_api.team.controller.TeamController;
import com.nn.ticketapp_api.team.exception.TeamAlreadyExistsException;
import com.nn.ticketapp_api.team.exception.TeamNotFoundException;
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
@RestControllerAdvice(assignableTypes = TeamController.class)
@RequiredArgsConstructor
public class TeamExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(TeamNotFoundException.class)
    public ProblemDetail handleTeamNotFoundException(TeamNotFoundException e, HttpServletRequest request) {
        log.warn("Team resource not found on path {}: {} ",request.getRequestURI(), e.getMessage() );

        return createProblemDetail(HttpStatus.NOT_FOUND, "Team Not Found", e.getMessage());
    }

    @ExceptionHandler(TeamAlreadyExistsException.class)
    public ProblemDetail handleTeamAlreadyExistsException(TeamAlreadyExistsException e, HttpServletRequest request) {
        log.warn("Team state conflict on path {}: {} ",request.getRequestURI(), e.getMessage() );

        return createProblemDetail(HttpStatus.CONFLICT, "Team Already Exists", e.getMessage());
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("timestamp", Instant.now(clock));

        return problemDetail;
    }
}
