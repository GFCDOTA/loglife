package com.loglife;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for integration tests: boots the full application against a real PostgreSQL provided by
 * Testcontainers (wired via {@code @ServiceConnection}). The Flyway migration runs and the schema
 * is validated by Hibernate ({@code ddl-auto=validate}). The container is static and the
 * {@code @SpringBootTest} configuration is identical across subclasses, so Spring caches the
 * context and the container starts only once for the whole IT suite.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
}
