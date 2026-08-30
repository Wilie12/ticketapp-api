package com.nn.ticketapp_api.team.service;

import com.nn.ticketapp_api.team.api.mapper.TeamMapper;
import com.nn.ticketapp_api.team.api.request.TeamCreateRequest;
import com.nn.ticketapp_api.team.api.request.TeamUpdateRequest;
import com.nn.ticketapp_api.team.api.response.TeamResponse;
import com.nn.ticketapp_api.team.domain.Team;
import com.nn.ticketapp_api.team.exception.TeamAlreadyExistsException;
import com.nn.ticketapp_api.team.exception.TeamNotFoundException;
import com.nn.ticketapp_api.team.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMapper teamMapper;
    @InjectMocks
    private TeamService teamService;

    @Test
    @DisplayName("Should create team and return response DTO")
    void shouldCreateTeamSuccessfully() {
        // given
        TeamCreateRequest request = new TeamCreateRequest("Network", "Handles network issues");
        Team savedTeam = Team.create(request.name(), request.description());
        TeamResponse expectedResponse = new TeamResponse(
                UUID.randomUUID(),
                "Network",
                "Handles network issues",
                null,
                null
        );

        given(teamRepository.existsByName(request.name())).willReturn(false);
        given(teamRepository.save(any(Team.class))).willReturn(savedTeam);
        given(teamMapper.toResponse(savedTeam)).willReturn(expectedResponse);

        // when
        TeamResponse actualResponse = teamService.createTeam(request);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.name()).isEqualTo("Network");
        assertThat(actualResponse.description()).isEqualTo("Handles network issues");

        then(teamRepository).should().existsByName(request.name());
        then(teamRepository).should().save(any(Team.class));
        then(teamMapper).should().toResponse(savedTeam);
    }

    @Test
    @DisplayName("Should throw TeamAlreadyExistsException when creating team with duplicate name")
    void shouldThrowExceptionWhenCreatingDuplicateTeam() {
        // given
        TeamCreateRequest request = new TeamCreateRequest("Network", "Desc");
        given(teamRepository.existsByName(request.name())).willReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> teamService.createTeam(request));

        // then
        assertThat(thrown).isInstanceOf(TeamAlreadyExistsException.class);

        then(teamRepository).should(never()).save(any(Team.class));
    }

    @Test
    @DisplayName("Should fetch existing team by Id and return response DTO")
    void shouldGetTeamSuccessfully() {
        // given
        UUID teamId = UUID.randomUUID();
        Team team = Team.create("Hardware", "Desc");
        TeamResponse expectedResponse = new TeamResponse(
                teamId,
                "Hardware",
                "Desc",
                null,
                null
        );

        given(teamRepository.findById(teamId)).willReturn(Optional.of(team));
        given(teamMapper.toResponse(team)).willReturn(expectedResponse);

        // when
        TeamResponse actualResponse = teamService.getTeam(teamId);

        // then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.id()).isEqualTo(teamId);
        assertThat(actualResponse.name()).isEqualTo("Hardware");

        then(teamRepository).should().findById(teamId);
        then(teamMapper).should().toResponse(team);
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when fetching non-existent team")
    void shouldThrowExceptionWhenGettingMissingTeam() {
        // given
        UUID teamId = UUID.randomUUID();
        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> teamService.getTeam(teamId));

        // then
        assertThat(thrown).isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    @DisplayName("Should return list of all teams mapped to response DTOs")
    void shouldGetAllTeamsSuccessfully() {
        // given
        Team team1 = Team.create("Network", "Network Desc");
        ReflectionTestUtils.setField(team1, "id", UUID.randomUUID());
        Team team2 = Team.create("Hardware", "Hardware Desc");
        ReflectionTestUtils.setField(team2, "id", UUID.randomUUID());

        TeamResponse response1 = new TeamResponse(
                team1.getId(),
                "Network",
                "Network Desc",
                null,
                null
        );
        TeamResponse response2 = new TeamResponse(
                team2.getId(),
                "Hardware",
                "Hardware Desc",
                null,
                null
        );

        given(teamRepository.findAll()).willReturn(List.of(team1, team2));
        given(teamMapper.toResponse(team1)).willReturn(response1);
        given(teamMapper.toResponse(team2)).willReturn(response2);

        // when
        List<TeamResponse> responses = teamService.getAllTeams();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TeamResponse::name).containsExactlyInAnyOrder("Network", "Hardware");
    }

    @Test
    @DisplayName("Should update team details using dirty checking")
    void shouldUpdateTeamSuccessfully() {
        // given
        UUID teamId = UUID.randomUUID();
        TeamUpdateRequest request = new TeamUpdateRequest("New Hardware", "New desc");
        Team existingTeam = Team.create("Hardware", "Old desc");
        TeamResponse expectedResponse = new TeamResponse(
                teamId,
                "New Hardware",
                "New desc",
                null,
                null
        );

        given(teamRepository.findById(teamId)).willReturn(Optional.of(existingTeam));
        given(teamRepository.existsByName(request.name())).willReturn(false);
        given(teamMapper.toResponse(existingTeam)).willReturn(expectedResponse);

        // when
        TeamResponse actualResponse = teamService.updateTeam(teamId, request);

        // then
        assertThat(actualResponse.name()).isEqualTo("New Hardware");
        assertThat(existingTeam.getName()).isEqualTo("New Hardware");

        then(teamRepository).should().findById(teamId);
        then(teamRepository).should().existsByName(request.name());
        then(teamRepository).should(never()).save(any(Team.class));
        then(teamMapper).should().toResponse(existingTeam);
    }

    @Test
    @DisplayName("Should throw TeamNotFoundException when updating non-existent team")
    void shouldThrowExceptionWhenUpdatingMissingTeam() {
        // given
        UUID teamId = UUID.randomUUID();
        TeamUpdateRequest request = new TeamUpdateRequest("Name", "Desc");

        given(teamRepository.findById(teamId)).willReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> teamService.updateTeam(teamId, request));

        // then
        assertThat(thrown).isInstanceOf(TeamNotFoundException.class);
    }
}
