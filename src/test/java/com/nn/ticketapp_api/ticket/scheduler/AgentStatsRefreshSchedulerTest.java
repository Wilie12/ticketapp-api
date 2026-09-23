package com.nn.ticketapp_api.ticket.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class AgentStatsRefreshSchedulerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @InjectMocks
    private AgentStatsRefreshScheduler agentStatsRefreshScheduler;

    @Test
    @DisplayName("Should execute concurrent refresh query without transaction block")
    void shouldRefreshMaterializedViewConcurrently() {
        // when
        agentStatsRefreshScheduler.refreshAgentStatsMaterializedView();

        // then
        then(jdbcTemplate).should().execute("REFRESH MATERIALIZED VIEW CONCURRENTLY agent_stats_mv");
    }
}
