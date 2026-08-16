package com.nn.ticketapp_api.admin.repository;

import com.nn.ticketapp_api.admin.domain.SlaConfiguration;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaConfigurationRepository extends JpaRepository<SlaConfiguration, TicketPriority> {
}
