package com.nn.ticketapp_api;

import com.nn.ticketapp_api.shared.identity.config.IdentityProperties;
import com.nn.ticketapp_api.shared.messaging.config.RabbitMqProperties;
import com.nn.ticketapp_api.shared.security.ratelimit.config.RateLimitProperties;
import com.nn.ticketapp_api.shared.storage.config.MinioProperties;
import com.nn.ticketapp_api.ticket.config.AnalyticsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        MinioProperties.class,
        IdentityProperties.class,
        AnalyticsProperties.class,
        RateLimitProperties.class,
        RabbitMqProperties.class
})
public class TicketappApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketappApiApplication.class, args);
    }

}
