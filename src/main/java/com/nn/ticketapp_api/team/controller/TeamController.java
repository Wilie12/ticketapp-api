package com.nn.ticketapp_api.team.controller;

import com.nn.ticketapp_api.shared.security.annotation.CurrentRequester;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import com.nn.ticketapp_api.team.api.request.TeamCreateRequest;
import com.nn.ticketapp_api.team.api.request.TeamUpdateRequest;
import com.nn.ticketapp_api.team.api.response.TeamResponse;
import com.nn.ticketapp_api.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TeamResponse createTeam(
            @Valid @RequestBody TeamCreateRequest request,
            @CurrentRequester RequesterContext context
    ) {
        log.info("Received request to create team with name: {} by admin: {}", request.name(), context.userId());

        return teamService.createTeam(request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<TeamResponse> getAllTeams(@CurrentRequester RequesterContext context) {
        log.info("Received request to fetch all teams by user: {}",context.userId());

        return teamService.getAllTeams();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public TeamResponse getTeam(
            @PathVariable(name = "id") UUID teamId,
            @CurrentRequester RequesterContext context
    ) {
        log.info("Received request to fetch team with id: {} by user: {}", teamId, context.userId());

        return teamService.getTeam(teamId);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public TeamResponse updateTeam(
            @PathVariable(name = "id") UUID teamId,
            @Valid @RequestBody TeamUpdateRequest request,
            @CurrentRequester RequesterContext context
    ) {
        log.info("Received request to update team with id: {} by admin: {}", teamId,  context.userId());

        return teamService.updateTeam(teamId, request);
    }
}
