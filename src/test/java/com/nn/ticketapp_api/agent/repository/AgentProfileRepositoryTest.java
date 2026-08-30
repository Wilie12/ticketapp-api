package com.nn.ticketapp_api.agent.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.agent.domain.AgentProfile;
import com.nn.ticketapp_api.agent.domain.AgentStatus;
import com.nn.ticketapp_api.team.domain.Team;
import com.nn.ticketapp_api.team.repository.TeamRepository;
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
    @Autowired
    private TeamRepository teamRepository;

    @AfterEach
    void tearDown() {
        agentProfileRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should successfully persist AgentProfile and retrieve it with default OFFLINE status")
    void shouldPersistAndRetrieveAgentProfile() {
        // given
        Team team = Team.create("Infrastructure", "Core IT");
        teamRepository.saveAndFlush(team);

        UUID agentId = UUID.randomUUID();
        AgentProfile agentProfile = AgentProfile.create(agentId, team.getId());

        // when
        agentProfileRepository.saveAndFlush(agentProfile);

        // then
        Optional<AgentProfile> retrievedProfile = agentProfileRepository.findById(agentId);
        assertThat(retrievedProfile).isPresent();
        assertThat(retrievedProfile.get().getTeamId()).isEqualTo(team.getId());
        assertThat(retrievedProfile.get().getStatus()).isEqualTo(AgentStatus.OFFLINE);
        assertThat(retrievedProfile.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should accurately update AgentProfile status via dirty checking mechanism")
    void shouldUpdateAgentStatus() {
        // given
        Team team = Team.create("Service Desk", "L1 Support");
        teamRepository.saveAndFlush(team);

        UUID agentId = UUID.randomUUID();
        AgentProfile agentProfile = AgentProfile.create(agentId, team.getId());
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
        Team targetTeam = Team.create("Hardware", "Net Ops");
        Team otherTeam = Team.create("Network", "HW Ops");
        teamRepository.saveAllAndFlush(List.of(targetTeam, otherTeam));

        UUID targetTeamId = targetTeam.getId();
        UUID otherTeamId = otherTeam.getId();

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
