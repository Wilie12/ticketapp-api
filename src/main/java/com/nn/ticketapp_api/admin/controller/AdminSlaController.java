package com.nn.ticketapp_api.admin.controller;

import com.nn.ticketapp_api.admin.api.request.SlaConfigurationUpdateRequest;
import com.nn.ticketapp_api.admin.api.response.SlaConfigurationResponse;
import com.nn.ticketapp_api.admin.service.SlaConfigurationService;
import com.nn.ticketapp_api.shared.security.annotation.CurrentRequester;
import com.nn.ticketapp_api.shared.security.domain.RequesterContext;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/sla-configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSlaController {

    private final SlaConfigurationService slaConfigurationService;

    @PutMapping("/{priority}")
    @ResponseStatus(HttpStatus.OK)
    public SlaConfigurationResponse updateSlaConfiguration(
            @PathVariable("priority") TicketPriority priority,
            @Valid @RequestBody SlaConfigurationUpdateRequest request,
            @CurrentRequester RequesterContext requesterContext
    ) {
        log.debug(
                "Received request to update SLA config for priority {} from admin: {}",
                priority,
                requesterContext.userId()
        );

        return slaConfigurationService.updateSlaConfiguration(priority, request.resolutionHours());
    }
}
