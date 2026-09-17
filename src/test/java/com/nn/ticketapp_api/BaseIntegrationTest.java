package com.nn.ticketapp_api;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.minio.MinioClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
public class BaseIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.0.7")
            .withRealmImportFile("keycloak/ticketapp-realm.json");

    static {
        postgres.start();
        keycloak.start();
    }

    @DynamicPropertySource
    static void keyCloakProperties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.keycloak.server-url", keycloak::getAuthServerUrl);
        registry.add("app.identity.keycloak.realm", () -> "ticketapp");
        registry.add("app.identity.keycloak.client-id", () -> "ticketapp-backend");
        registry.add("app.identity.keycloak.client-secret",() -> "secret");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/ticketapp");

    }

    @MockitoBean
    protected JwtDecoder jwtDecoder;
    @MockitoBean
    protected MinioClient minioClient;

}
