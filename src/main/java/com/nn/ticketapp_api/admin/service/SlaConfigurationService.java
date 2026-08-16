package com.nn.ticketapp_api.admin.service;

import com.nn.ticketapp_api.admin.api.mapper.SlaConfigurationMapper;
import com.nn.ticketapp_api.admin.api.response.SlaConfigurationResponse;
import com.nn.ticketapp_api.admin.domain.SlaConfiguration;
import com.nn.ticketapp_api.admin.repository.SlaConfigurationRepository;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaConfigurationService {

    private final SlaConfigurationRepository slaConfigurationRepository;
    private final SlaConfigurationMapper slaConfigurationMapper;

    private static final Map<TicketPriority, Integer> DEFAULT_SLA_HOURS = Map.of(
            TicketPriority.CRITICAL, 2,
            TicketPriority.HIGH, 4,
            TicketPriority.MEDIUM, 24,
            TicketPriority.LOW, 48
    );

    @Transactional
    public Integer getResolutionHours(TicketPriority priority) {
        log.debug("Fetching SLA configuration for priority {}", priority);

        return slaConfigurationRepository.findById(priority)
                .map(SlaConfiguration::getResolutionHours)
                .orElseGet(() -> {
                    log.warn("SLA configuration not found for priority {}. Using default value.", priority);
                    return DEFAULT_SLA_HOURS.get(priority);
                });
    }

    @Transactional
    public SlaConfigurationResponse updateSlaConfiguration(TicketPriority priority, Integer newResolutionHours) {
        log.debug("Updating SLA configuration for priority {} to {} hours", priority, newResolutionHours);

        SlaConfiguration configuration = slaConfigurationRepository.findById(priority)
                .map(existingConfig -> {
                    existingConfig.updateResolutionHours(newResolutionHours);
                    return existingConfig;
                })
                .orElseGet(() -> SlaConfiguration.create(priority, newResolutionHours));

        SlaConfiguration savedConfiguration = slaConfigurationRepository.save(configuration);

        return slaConfigurationMapper.toResponse(savedConfiguration);
    }
}
