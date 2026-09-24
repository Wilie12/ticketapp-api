package com.nn.ticketapp_api.ticket.controller;

import com.nn.ticketapp_api.shared.api.response.PageResponse;
import com.nn.ticketapp_api.shared.security.annotation.CurrentRequester;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import com.nn.ticketapp_api.ticket.api.response.StatsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.service.AgentStatsService;
import com.nn.ticketapp_api.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AgentStatsService agentStatsService;
    private final TicketService ticketService;

    @GetMapping("/stats/agents")
    @ResponseStatus(HttpStatus.OK)
    public List<StatsResponse> getAllAgentsStats(@CurrentRequester RequesterContext requesterContext) {
        log.debug("Received request to get global agent statistics from admin: {}", requesterContext.userId());

        return agentStatsService.getAllAgentsStats();
    }

    @GetMapping("/tickets")
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<TicketResponse> getAllTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug("Received request to fetch all tickets (global view) from admin: {}", requesterContext.userId());

        return ticketService.getAllTickets(pageable);
    }
}
