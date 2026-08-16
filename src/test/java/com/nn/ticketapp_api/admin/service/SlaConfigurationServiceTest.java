package com.nn.ticketapp_api.admin.service;

import com.nn.ticketapp_api.admin.api.mapper.SlaConfigurationMapper;
import com.nn.ticketapp_api.admin.api.response.SlaConfigurationResponse;
import com.nn.ticketapp_api.admin.domain.SlaConfiguration;
import com.nn.ticketapp_api.admin.repository.SlaConfigurationRepository;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class SlaConfigurationServiceTest {

    @Mock
    private SlaConfigurationRepository slaConfigurationRepository;
    @Mock
    private SlaConfigurationMapper slaConfigurationMapper;
    @InjectMocks
    private SlaConfigurationService slaConfigurationService;

    @Test
    @DisplayName("Should return configured resolution hours if present in database")
    void shouldReturnConfiguredHoursIfPresent() {
        // given
        SlaConfiguration mockConfig = SlaConfiguration.create(TicketPriority.CRITICAL, 1);
        given(slaConfigurationRepository.findById(TicketPriority.CRITICAL)).willReturn(Optional.of(mockConfig));

        // when
        Integer hours = slaConfigurationService.getResolutionHours(TicketPriority.CRITICAL);

        // then
        assertThat(hours).isEqualTo(1);

        then(slaConfigurationRepository).should().findById(TicketPriority.CRITICAL);
    }

    @Test
    @DisplayName("Should gracefully fallback to default hours if configuration is missing")
    void shouldFallbackToGracefulDegradation() {
        // given
        given(slaConfigurationRepository.findById(TicketPriority.MEDIUM)).willReturn(Optional.empty());

        // when
        Integer hours = slaConfigurationService.getResolutionHours(TicketPriority.MEDIUM);

        // then
        assertThat(hours).isEqualTo(24);
        then(slaConfigurationRepository).should().findById(TicketPriority.MEDIUM);
    }

    @Test
    @DisplayName("Should create new configuration and map to DTO if it does not exist during update")
    void shouldCreateNewConfigurationOnUpdate() {
        // given
        given(slaConfigurationRepository.findById(TicketPriority.HIGH)).willReturn(Optional.empty());

        SlaConfiguration expectedConfiguration = SlaConfiguration.create(TicketPriority.HIGH, 10);
        SlaConfigurationResponse expectedResponse = new SlaConfigurationResponse(TicketPriority.HIGH, 10);

        given(slaConfigurationRepository.save(any(SlaConfiguration.class))).willReturn(expectedConfiguration);
        given(slaConfigurationMapper.toResponse(expectedConfiguration)).willReturn(expectedResponse);

        // when
        SlaConfigurationResponse actualResponse = slaConfigurationService
                .updateSlaConfiguration(TicketPriority.HIGH, 10);

        // then
        assertThat(actualResponse.resolutionHours()).isEqualTo(10);

        then(slaConfigurationRepository).should().save(any(SlaConfiguration.class));
        then(slaConfigurationMapper).should().toResponse(expectedConfiguration);
    }
}
