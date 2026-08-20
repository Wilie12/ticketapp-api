package com.nn.ticketapp_api.agent.service;

import com.nn.ticketapp_api.agent.api.mapper.AgentProfileMapper;
import com.nn.ticketapp_api.agent.api.response.AgentProfileResponse;
import com.nn.ticketapp_api.agent.domain.AgentProfile;
import com.nn.ticketapp_api.agent.domain.AgentStatus;
import com.nn.ticketapp_api.agent.domain.event.AgentAvailableEvent;
import com.nn.ticketapp_api.agent.exception.AgentNotFoundException;
import com.nn.ticketapp_api.agent.exception.AgentProfileAlreadyExistsException;
import com.nn.ticketapp_api.agent.repository.AgentProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class AgentProfileServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-01T12:00:00Z");

    @Mock
    private AgentProfileRepository agentProfileRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private AgentProfileMapper agentProfileMapper;
    @InjectMocks
    private AgentProfileService agentProfileService;

    @Test
    @DisplayName("Should successfully provision a new agent profile and return mapped DTO")
    void shouldProvisionAgentProfile() {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        AgentProfile profile = AgentProfile.create(agentId, teamId);
        AgentProfileResponse expectedResponse = new AgentProfileResponse(
                agentId,
                teamId,
                AgentStatus.OFFLINE,
                FIXED_NOW
        );

        given(agentProfileRepository.existsById(agentId)).willReturn(false);
        given(agentProfileRepository.save(any(AgentProfile.class))).willReturn(profile);
        given(agentProfileMapper.toResponse(profile)).willReturn(expectedResponse);

        // when
        AgentProfileResponse actualResponse = agentProfileService.createAgentProfile(agentId, teamId);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.agentId()).isEqualTo(agentId);
        assertThat(actualResponse.teamId()).isEqualTo(teamId);
        assertThat(actualResponse.status()).isEqualTo(AgentStatus.OFFLINE);

        then(agentProfileRepository).should().existsById(agentId);
        then(agentProfileRepository).should().save(any(AgentProfile.class));
        then(agentProfileMapper).should().toResponse(profile);
    }

    @Test
    @DisplayName("Should throw AgentProfileAlreadyExistsException when provisioning duplicate agent")
    void shouldRejectDuplicateProvisioning() {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();

        given(agentProfileRepository.existsById(agentId)).willReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> agentProfileService.createAgentProfile(agentId, teamId));

        assertThat(thrown)
                .isInstanceOf(AgentProfileAlreadyExistsException.class)
                .hasMessageContaining(String.format("Agent profile with ID %s already exists", agentId));

        then(agentProfileRepository).should().existsById(agentId);
        then(agentProfileRepository).should(never()).save(any(AgentProfile.class));
        then(agentProfileMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should update status and publish AgentAvailableEvent when transitioning to AVAILABLE")
    void shouldUpdateStatusAndPublishEventWhenAgentBecomesAvailable() {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        AgentProfile profile = AgentProfile.create(agentId, teamId);

        given(agentProfileRepository.findById(agentId)).willReturn(Optional.of(profile));

        // when
        agentProfileService.updateStatus(agentId, AgentStatus.AVAILABLE);

        // then
        assertThat(profile.getStatus()).isEqualTo(AgentStatus.AVAILABLE);

        then(agentProfileRepository).should().findById(agentId);
        then(applicationEventPublisher).should().publishEvent(new AgentAvailableEvent(agentId, teamId));
    }

    @Test
    @DisplayName("Should update status but NOT publish AgentAvailableEvent when transitioning to BUSY")
    void shouldUpdateStatusWithoutPublishingEventWhenAgentBecomeBusy() {
        // given
        UUID agentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        AgentProfile profile = AgentProfile.create(agentId, teamId);

        given(agentProfileRepository.findById(agentId)).willReturn(Optional.of(profile));

        // when
        agentProfileService.updateStatus(agentId, AgentStatus.BUSY);

        // then
        assertThat(profile.getStatus()).isEqualTo(AgentStatus.BUSY);

        then(agentProfileRepository).should().findById(agentId);
        then(applicationEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should throw AgentNotFoundException when updating status for non-existent agent")
    void shouldThrowAgentNotFoundExceptionWhenUpdatingStatusForUnknownAgent() {
        // given
        UUID agentId = UUID.randomUUID();

        given(agentProfileRepository.findById(agentId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> agentProfileService.updateStatus(agentId, AgentStatus.AVAILABLE));

        // then
        assertThat(thrown)
                .isInstanceOf(AgentNotFoundException.class)
                .hasMessageContaining(String.format("Agent profile with ID: %s not found", agentId));

        then(agentProfileRepository).should().findById(agentId);
        then(applicationEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Sould return list of available agent UUIDs for given team")
    void shouldReturnListOfAvailableAgentsForGivenTeam() {
        // given
        UUID teamId = UUID.randomUUID();
        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();

        AgentProfile profile1 = AgentProfile.create(agent1, teamId);
        profile1.updateStatus(AgentStatus.AVAILABLE);

        AgentProfile profile2 = AgentProfile.create(agent2, teamId);
        profile2.updateStatus(AgentStatus.AVAILABLE);

        given(agentProfileRepository.findByTeamIdAndStatus(teamId, AgentStatus.AVAILABLE))
                .willReturn(List.of(profile1, profile2));

        // when
        List<UUID> availableAgents = agentProfileService.getAvailableAgentsForTeam(teamId);

        // then
        assertThat(availableAgents).hasSize(2);
        assertThat(availableAgents).containsExactlyInAnyOrder(agent1, agent2);

        then(agentProfileRepository).should().findByTeamIdAndStatus(teamId, AgentStatus.AVAILABLE);
    }
}
