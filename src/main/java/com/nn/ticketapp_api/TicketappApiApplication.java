package com.nn.ticketapp_api;

import com.nn.ticketapp_api.shared.storage.config.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MinioProperties.class)
public class TicketappApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketappApiApplication.class, args);
	}

}
