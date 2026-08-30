package com.nn.ticketapp_api.team.api.mapper;

import com.nn.ticketapp_api.team.api.response.TeamResponse;
import com.nn.ticketapp_api.team.domain.Team;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeamMapper {
    TeamResponse toResponse(Team team);
}
