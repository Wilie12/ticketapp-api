package com.nn.ticketapp_api.ticket.api.mapper;

import com.nn.ticketapp_api.communication.api.response.AttachmentResponse;
import com.nn.ticketapp_api.communication.api.response.CommunicationResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketDetailsResponse;
import com.nn.ticketapp_api.ticket.api.response.TicketResponse;
import com.nn.ticketapp_api.ticket.domain.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketMapper {
    TicketResponse toResponse(Ticket ticket);

    @Mapping(target = "id", source = "ticket.id")
    @Mapping(target = "comments", source = "comments")
    @Mapping(target = "workNotes", source = "workNotes")
    @Mapping(target = "systemEvents", source = "systemEvents")
    @Mapping(target = "attachments", source = "attachments")
    TicketDetailsResponse toDetailsResponse(
            Ticket ticket,
            List<CommunicationResponse> comments,
            List<CommunicationResponse> workNotes,
            List<CommunicationResponse> systemEvents,
            List<AttachmentResponse> attachments
    );
}
