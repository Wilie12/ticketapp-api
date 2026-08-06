package com.nn.ticketapp_api.ticket.api.mapper;

import com.nn.ticketapp_api.ticket.api.response.StatsResponse;
import com.nn.ticketapp_api.ticket.repository.projection.AgentStatsProjection;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AgentStatsMapper {
    StatsResponse toResponse(AgentStatsProjection projection);
}
