package com.nn.ticketapp_api.admin.api.mapper;

import com.nn.ticketapp_api.admin.api.response.SlaConfigurationResponse;
import com.nn.ticketapp_api.admin.domain.SlaConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SlaConfigurationMapper {
    SlaConfigurationResponse toResponse(SlaConfiguration slaConfiguration);
}
