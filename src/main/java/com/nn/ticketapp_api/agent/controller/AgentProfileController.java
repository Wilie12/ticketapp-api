package com.nn.ticketapp_api.agent.controller;

import com.nn.ticketapp_api.agent.api.request.AgentProfileCreateRequest;
import com.nn.ticketapp_api.agent.api.response.AgentProfileResponse;
import com.nn.ticketapp_api.agent.service.AgentProfileService;
import com.nn.ticketapp_api.shared.security.annotation.CurrentRequester;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AgentProfileController {

    private final AgentProfileService agentProfileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentProfileResponse provisionAgent(
            @Valid @RequestBody AgentProfileCreateRequest request,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug(
                "Received request to provision agent profile {} from admin: {}",
                request.agentId(),
                requesterContext.userId()
        );

        return agentProfileService.createAgentProfile(request.agentId(), request.teamId());
    }
}
