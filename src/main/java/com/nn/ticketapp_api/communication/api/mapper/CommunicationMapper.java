package com.nn.ticketapp_api.communication.api.mapper;

import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.communication.domain.Communication;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommunicationMapper {
    CommunicationResponse toResponse(Communication communication);
}
