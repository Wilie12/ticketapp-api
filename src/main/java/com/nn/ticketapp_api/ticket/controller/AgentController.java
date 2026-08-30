package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.agent.api.request.AgentStatusUpdateRequest;
import com.nn.ticketapp_api.agent.service.AgentProfileService;
import com.nn.ticketapp_api.shared.api.response.PageResponse;
import com.nn.ticketapp_api.shared.security.annotation.CurrentRequester;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import com.nn.ticketapp_api.ticket.api.response.StatsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.service.AgentStatsService;
import com.nn.ticketapp_api.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AgentController {

    private final TicketService ticketService;
    private final AgentStatsService agentStatsService;
    private final AgentProfileService agentProfileService;

    @GetMapping("/me/tickets")
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<TicketResponse> getAgentTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @CurrentRequester RequesterContext requesterContext
    ) {
        requesterContext.requireInternal();

        log.debug("Received request to get assigned tickets for agent: {}", requesterContext.userId());

        return ticketService.getAgentTickets(requesterContext.userId(), pageable);
    }

    @GetMapping("/me/stats")
    @ResponseStatus(HttpStatus.OK)
    public StatsResponse getAgentStats(@CurrentRequester RequesterContext requesterContext) {
        requesterContext.requireInternal();

        log.debug("Received request to get statistics for agent: {}", requesterContext.userId());

        return agentStatsService.getAgentStats(requesterContext.userId());
    }

    @PutMapping("/me/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAgentStatus(
            @Valid @RequestBody AgentStatusUpdateRequest agentStatusUpdateRequest,
            @CurrentRequester RequesterContext requesterContext
    ) {
        requesterContext.requireInternal();

        log.debug(
                "Received request to update status for agent {} to {}",
                requesterContext.userId(),
                agentStatusUpdateRequest.status()
        );

        agentProfileService.updateStatus(requesterContext.userId(), agentStatusUpdateRequest.status());
    }
}
