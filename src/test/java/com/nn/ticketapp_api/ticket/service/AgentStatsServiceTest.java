package com.nn.ticketapp_api.ticket.service;

import com.nn.ticketapp_api.ticket.api.mapper.AgentStatsMapper;
import com.nn.ticketapp_api.ticket.api.response.StatsResponse;
import com.nn.ticketapp_api.ticket.repository.AgentStatsRepository;
import com.nn.ticketapp_api.ticket.repository.projection.AgentStatsProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class AgentStatsServiceTest {

    @Mock
    private AgentStatsRepository agentStatsRepository;
    @Mock
    private AgentStatsMapper agentStatsMapper;
    @InjectMocks
    private AgentStatsService agentStatsService;

    @Test
    @DisplayName("Should return agent stats if found in materialized view")
    void shouldReturnAgentStats() {
        // given
        UUID agentId = UUID.randomUUID();
        AgentStatsProjection mockProjection = mock(AgentStatsProjection.class);
        StatsResponse expectedResponse = new StatsResponse(agentId, 5L, 10L, 1L);

        given(agentStatsRepository.findStatsByAgentId(agentId)).willReturn(Optional.of(mockProjection));
        given(agentStatsMapper.toResponse(mockProjection)).willReturn(expectedResponse);

        // when
        StatsResponse actualResponse = agentStatsService.getAgentStats(agentId);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.currentOpenTicketsCount()).isEqualTo(5L);
        assertThat(actualResponse.allResolvedTicketsCount()).isEqualTo(10L);
        assertThat(actualResponse.allSlaBreachedCount()).isEqualTo(1L);

        then(agentStatsRepository).should().findStatsByAgentId(agentId);
        then(agentStatsMapper).should().toResponse(mockProjection);
    }

    @Test
    @DisplayName("Should return zeroed stats if agent not found in materialized view")
    void shouldReturnZeroedStatsIfNotFound() {
        // given
        UUID agentId = UUID.randomUUID();
        given(agentStatsRepository.findStatsByAgentId(agentId)).willReturn(Optional.empty());

        // when
        StatsResponse actualResponse = agentStatsService.getAgentStats(agentId);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.agentId()).isEqualTo(agentId);
        assertThat(actualResponse.currentOpenTicketsCount()).isEqualTo(0L);
        assertThat(actualResponse.allResolvedTicketsCount()).isEqualTo(0L);
        assertThat(actualResponse.allSlaBreachedCount()).isEqualTo(0L);

        then(agentStatsRepository).should().findStatsByAgentId(agentId);
        then(agentStatsMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should return list od statistics for all agents from materialized view")
    void shouldReturnAllAgentsStats() {
        // given
        AgentStatsProjection mockProjection = mock(AgentStatsProjection.class);
        StatsResponse mockResponse = new StatsResponse(UUID.randomUUID(), 2L, 5L, 0L);

        given(agentStatsRepository.findAllAgentStats()).willReturn(List.of(mockProjection));
        given(agentStatsMapper.toResponse(mockProjection)).willReturn(mockResponse);

        // when
        List<StatsResponse> results = agentStatsService.getAllAgentsStats();

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(mockResponse);

        then(agentStatsRepository).should().findAllAgentStats();
        then(agentStatsMapper).should().toResponse(mockProjection);
    }
}
