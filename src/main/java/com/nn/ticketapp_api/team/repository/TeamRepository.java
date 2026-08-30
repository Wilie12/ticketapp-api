package com.nn.ticketapp_api.team.repository;

import com.nn.ticketapp_api.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    boolean existsByName(String name);
    Optional<Team> findByName(String name);
}
