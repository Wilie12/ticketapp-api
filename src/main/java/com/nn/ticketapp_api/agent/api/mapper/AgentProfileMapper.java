package com.nn.ticketapp_api.agent.api.mapper;

import com.nn.ticketapp_api.agent.api.response.AgentProfileResponse;
import com.nn.ticketapp_api.agent.domain.AgentProfile;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AgentProfileMapper {
    AgentProfileResponse toResponse(AgentProfile agentProfile);
}
