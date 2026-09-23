package com.nn.ticketapp_api.ticket.service;

import com.nn.ticketapp_api.ticket.api.mapper.AgentStatsMapper;
import com.nn.ticketapp_api.ticket.api.response.StatsResponse;
import com.nn.ticketapp_api.ticket.repository.AgentStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentStatsService {

    private final AgentStatsRepository agentStatsRepository;
    private final AgentStatsMapper agentStatsMapper;

    @Transactional(readOnly = true)
    public StatsResponse getAgentStats(UUID agentId) {
        log.debug("Retrieving statistics for agent: {} from materialized view", agentId);

        return agentStatsRepository.findStatsByAgentId(agentId)
                .map(agentStatsMapper::toResponse)
                .orElseGet(() -> {
                            log.debug("No tickets found for agent: {}. Returning zeroed statistics.", agentId);
                            return new StatsResponse(agentId, 0, 0, 0);
                        }
                );
    }

    @Transactional(readOnly = true)
    public List<StatsResponse> getAllAgentsStats() {
        log.debug("Retrieving global statistics for all agents from materialized view.");

        return agentStatsRepository.findAllAgentStats()
                .stream()
                .map(agentStatsMapper::toResponse)
                .toList();
    }
}
