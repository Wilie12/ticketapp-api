package com.nn.ticketapp_api.agent.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.agent.domain.AgentProfile;
import com.nn.ticketapp_api.agent.domain.AgentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AgentProfileRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AgentProfileRepository agentProfileRepository;

    @AfterEach
    void tearDown() {
        agentProfileRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should successfully persist AgentProfile and retrieve it with default OFFLINE status")
    void shouldPersistAndRetrieveAgentProfile() {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        AgentProfile agentProfile = AgentProfile.create(agentId, teamId);

        // when
        agentProfileRepository.saveAndFlush(agentProfile);

        // then
        Optional<AgentProfile> retrievedProfile = agentProfileRepository.findById(agentId);
        assertThat(retrievedProfile).isPresent();
        assertThat(retrievedProfile.get().getTeamId()).isEqualTo(teamId);
        assertThat(retrievedProfile.get().getStatus()).isEqualTo(AgentStatus.OFFLINE);
        assertThat(retrievedProfile.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should accurately update AgentProfile status via dirty checking mechanism")
    void shouldUpdateAgentStatus() {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        AgentProfile agentProfile = AgentProfile.create(agentId, teamId);
        agentProfileRepository.saveAndFlush(agentProfile);

        AgentProfile persistedProfile = agentProfileRepository.findById(agentId).orElseThrow();

        // when
        persistedProfile.updateStatus(AgentStatus.AVAILABLE);
        agentProfileRepository.saveAndFlush(persistedProfile);

        // then
        AgentProfile updatedProfile = agentProfileRepository.findById(agentId).orElseThrow();
        assertThat(updatedProfile.getStatus()).isEqualTo(AgentStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should retrieve only agents matching both team ID and specific status")
    void shouldFindByTeamIdAndStatus() {
        // given
        UUID targetTeamId = UUID.randomUUID();
        UUID otherTeamId = UUID.randomUUID();

        AgentProfile availableTargetAgent = AgentProfile.create(UUID.randomUUID(), targetTeamId);
        availableTargetAgent.updateStatus(AgentStatus.AVAILABLE);
        agentProfileRepository.save(availableTargetAgent);

        AgentProfile offlineTargetAgent = AgentProfile.create(UUID.randomUUID(), targetTeamId);
        agentProfileRepository.save(offlineTargetAgent);

        AgentProfile availableOtherAgent = AgentProfile.create(UUID.randomUUID(), otherTeamId);
        availableOtherAgent.updateStatus(AgentStatus.AVAILABLE);
        agentProfileRepository.save(availableOtherAgent);

        agentProfileRepository.flush();

        // when
        List<AgentProfile> results = agentProfileRepository.findByTeamIdAndStatus(targetTeamId, AgentStatus.AVAILABLE);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(availableTargetAgent.getId());
        assertThat(results.get(0).getTeamId()).isEqualTo(offlineTargetAgent.getTeamId());
        assertThat(results.get(0).getStatus()).isEqualTo(AgentStatus.AVAILABLE);
    }
}
