package com.nn.ticketapp_api.team.api.advice;

import com.nn.ticketapp_api.team.controller.TeamController;
import com.nn.ticketapp_api.team.exception.TeamAlreadyExistsException;
import com.nn.ticketapp_api.team.exception.TeamNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = TeamController.class)
public class TeamExceptionHandler {

    @ExceptionHandler(TeamNotFoundException.class)
    public ProblemDetail handleTeamNotFoundException(TeamNotFoundException ex) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Team Not Found");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(TeamAlreadyExistsException.class)
    public ProblemDetail handleTeamAlreadyExistsException(TeamAlreadyExistsException ex) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Team Already Exists");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
