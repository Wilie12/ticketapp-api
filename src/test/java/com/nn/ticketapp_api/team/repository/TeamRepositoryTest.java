package com.nn.ticketapp_api.team.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.team.domain.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class TeamRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TeamRepository teamRepository;

    @AfterEach
    public void tearDown() {
        teamRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should successfully persist Team and Retrieve it by ID")
    void shouldPersistAndRetrieveTeamById() {
        // given
        Team team = Team.create("Network Team", "Handles VPN and router issues");

        // when
        Team savedTeam = teamRepository.saveAndFlush(team);

        // then
        Optional<Team> retrievedTeam = teamRepository.findById(savedTeam.getId());

        assertThat(retrievedTeam).isPresent();
        assertThat(retrievedTeam.get().getName()).isEqualTo("Network Team");
        assertThat(retrievedTeam.get().getDescription()).isEqualTo("Handles VPN and router issues");
        assertThat(retrievedTeam.get().getCreatedAt()).isNotNull();
        assertThat(retrievedTeam.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update Team details using Hibernate dirty checking")
    void shouldUpdateTeamDetails() {
        // given
        Team team = Team.create("Hardware", "Hardware issues");
        teamRepository.saveAndFlush(team);

        Team persistedTeam = teamRepository.findById(team.getId()).orElseThrow();

        // when
        persistedTeam.updateDetails("Hardware Team", "Handles physical equipment");
        teamRepository.saveAndFlush(persistedTeam);

        // then
        Team updatedTeam = teamRepository.findById(team.getId()).orElseThrow();
        assertThat(updatedTeam.getName()).isEqualTo("Hardware Team");
        assertThat(updatedTeam.getDescription()).isEqualTo("Handles physical equipment");
    }

    @Test
    @DisplayName("Should throw exception when attempting to create a team with duplicate name")
    void shouldThrowExceptionOnDuplicateTeamName() {
        // given
        Team firstTeam = Team.create("Helpdesk", "First line of support");
        teamRepository.saveAndFlush(firstTeam);

        Team duplicateTeam = Team.create("Helpdesk", "Different description");

        // when
        Throwable thrown = catchThrowable(() -> teamRepository.saveAndFlush(duplicateTeam));

        // then
        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);
    }
}
