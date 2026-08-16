package com.nn.ticketapp_api.admin.repository;

import com.nn.ticketapp_api.BaseIntegrationTest;
import com.nn.ticketapp_api.admin.domain.SlaConfiguration;
import com.nn.ticketapp_api.ticket.domain.TicketPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SlaConfigurationRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private SlaConfigurationRepository slaConfigurationRepository;

    @AfterEach
    public void tearDown() {
        slaConfigurationRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Should successully persist SlaConfiguration and retrieve it by priority")
    void shouldPersistAndRetrieveSlaConfiguration() {
        // given
        TicketPriority priority = TicketPriority.MEDIUM;
        Integer resolutionHours = 4;
        SlaConfiguration configuration = SlaConfiguration.create(priority, resolutionHours);

        // when
        slaConfigurationRepository.saveAndFlush(configuration);

        // then
        Optional<SlaConfiguration> retrievedConfiguration = slaConfigurationRepository.findById(priority);
        assertThat(retrievedConfiguration).isPresent();
        assertThat(retrievedConfiguration.get().getPriority()).isEqualTo(priority);
        assertThat(retrievedConfiguration.get().getResolutionHours()).isEqualTo(resolutionHours);
    }

    @Test
    @DisplayName("Should accurately update SlaConfiguration hours via dirty checking mechanism")
    void shouldUpdateSlaConfigurationResolutionHours() {
        // given
        TicketPriority priority = TicketPriority.HIGH;
        SlaConfiguration configuration = SlaConfiguration.create(priority, 2);
        slaConfigurationRepository.saveAndFlush(configuration);

        SlaConfiguration persistedConfiguration = slaConfigurationRepository.findById(priority).orElseThrow();

        // when
        persistedConfiguration.updateResolutionHours(1);
        slaConfigurationRepository.saveAndFlush(persistedConfiguration);

        // then
        SlaConfiguration updatedConfiguration = slaConfigurationRepository.findById(priority).orElseThrow();
        assertThat(updatedConfiguration.getResolutionHours()).isEqualTo(1);
    }
}
