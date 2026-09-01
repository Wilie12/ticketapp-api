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

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Team Not Found");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(TeamAlreadyExistsException.class)
    public ProblemDetail handleTeamAlreadyExistsException(TeamAlreadyExistsException e, HttpServletRequest request) {
        log.warn("Team state conflict on path {}: {} ",request.getRequestURI(), e.getMessage() );

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problemDetail.setTitle("Team Already Exists");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
