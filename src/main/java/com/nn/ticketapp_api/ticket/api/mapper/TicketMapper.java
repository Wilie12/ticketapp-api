package com.nn.ticketapp_api.ticket.api.mapper;

import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketMapper {
    TicketResponse toResponse(Ticket ticket);
    TicketDetailsResponse toDetailsResponse(Ticket ticket);
}
