package com.nn.ticketapp_api.team.service;

import com.nn.ticketapp_api.team.api.mapper.TeamMapper;
import com.nn.ticketapp_api.team.api.request.TeamCreateRequest;
import com.nn.ticketapp_api.team.api.request.TeamUpdateRequest;
import com.nn.ticketapp_api.team.api.response.TeamResponse;
import com.nn.ticketapp_api.team.domain.Team;
import com.nn.ticketapp_api.team.exception.TeamAlreadyExistsException;
import com.nn.ticketapp_api.team.exception.TeamNotFoundException;
import com.nn.ticketapp_api.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;

    @Transactional
    public TeamResponse createTeam(TeamCreateRequest request) {
        log.info("Creating new team with name: {}", request.name());

        if (teamRepository.existsByName(request.name())) {
            throw new TeamAlreadyExistsException(request.name());
        }

        Team team = Team.create(request.name(), request.description());
        Team savedTeam = teamRepository.save(team);

        return teamMapper.toResponse(savedTeam);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(UUID id) {
        log.debug("Fetching team with id: {}", id);

        return teamRepository.findById(id)
                .map(teamMapper::toResponse)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        log.debug("Fetching all teams");

        return teamRepository.findAll().stream()
                .map(teamMapper::toResponse)
                .toList();
    }

    @Transactional
    public TeamResponse updateTeam(UUID id, TeamUpdateRequest request) {
        log.info("Updating team with id: {}", id);

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));

        Optional.ofNullable(request.name())
                .filter(name -> !name.equals(team.getName()))
                .filter(teamRepository::existsByName)
                .ifPresent(name -> {
                    throw new TeamAlreadyExistsException(name);
                });

        team.updateDetails(request.name(), request.description());

        return teamMapper.toResponse(team);
    }
}
