package com.nn.ticketapp_api.ticket.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentStatsRefreshScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRateString = "${app.analytics.refresh-rate}")
    public void refreshAgentStatsMaterializedView() {
        log.debug("Starting concurrent refresh of agent_stats_mv materialized view");

        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY agent_stats_mv");
            log.info("Successfully refreshed agent_stats_mv materialized view in background");
        } catch (Exception e) {
            log.error("Failed to concurrently refresh agent_stats_mv materialized view." +
                    " Analytics read-model might be stale.", e);
        }
    }
}
