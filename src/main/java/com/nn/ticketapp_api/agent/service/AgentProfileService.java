package com.nn.ticketapp_api.agent.service;

import com.nn.ticketapp_api.agent.domain.AgentProfile;
import com.nn.ticketapp_api.agent.domain.AgentStatus;
import com.nn.ticketapp_api.agent.domain.event.AgentAvailableEvent;
import com.nn.ticketapp_api.agent.exception.AgentNotFoundException;
import com.nn.ticketapp_api.agent.repository.AgentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProfileService {

    private final AgentProfileRepository agentProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void updateStatus(UUID agentId, AgentStatus newStatus) {
        log.debug("Updating status for agent {} to {}", agentId, newStatus);

        AgentProfile profile = agentProfileRepository.findById(agentId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));

        boolean isStatusChanged = !profile.getStatus().equals(newStatus);

        profile.updateStatus(newStatus);

        if (isStatusChanged && newStatus == AgentStatus.AVAILABLE) {
            log.info("Agent {} is now AVAILABLE. Publishing event to evaluate unassigned queue.", agentId);
            eventPublisher.publishEvent(new AgentAvailableEvent(agentId, profile.getTeamId()));
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> getAvailableAgentsForTeam(UUID teamId) {
        return agentProfileRepository.findByTeamIdAndStatus(teamId, AgentStatus.AVAILABLE)
                .stream()
                .map(AgentProfile::getId)
                .toList();
    }
}
