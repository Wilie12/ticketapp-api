package com.nn.ticketapp_api.notification.template;

import com.nn.ticketapp_api.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailTemplateProcessorTest extends BaseIntegrationTest {

    @Autowired
    private EmailTemplateProcessor emailTemplateProcessor;

    @Test
    @DisplayName("Should successfully process ticket-created template with provided variables")
    void shouldProcessTicketCreatedTemplate() {
        // given
        Map<String, Object> variables = Map.of(
                "ticketNumber", "INC0000123",
                "title", "Cannot connect to VPN",
                "priority", "HIGH"
        );

        // when
        String result = emailTemplateProcessor.processTemplate("email/ticket-created", variables);

        // then
        assertThat(result)
                .contains("INC0000123")
                .contains("Cannot connect to VPN")
                .contains("HIGH");
    }

    @Test
    @DisplayName("Should process template safely and not throw exceptions when variables map is null")
    void shouldProcessTemplateWithNullVariables() {
        // given

        // when
        String result = emailTemplateProcessor.processTemplate("email/ticket-created", null);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("Ticket Created");
    }
}
