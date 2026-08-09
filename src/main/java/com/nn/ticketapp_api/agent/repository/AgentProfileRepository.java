package com.nn.ticketapp_api.agent.repository;

import com.nn.ticketapp_api.agent.domain.AgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AgentProfileRepository extends JpaRepository<AgentProfile, UUID> {
}
