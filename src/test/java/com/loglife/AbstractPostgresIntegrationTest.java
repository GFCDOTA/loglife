package com.loglife;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for integration tests: boots the full application against a real PostgreSQL provided by
 * Testcontainers (wired via {@code @ServiceConnection}). The Flyway migration runs and the schema
 * is validated by Hibernate ({@code ddl-auto=validate}).
 *
 * <p>The container is a singleton started once for the whole JVM (Ryuk reaps it on exit). It is
 * deliberately NOT managed by the {@code @Testcontainers}/{@code @Container} extension: that
 * lifecycle stops a static container after the FIRST test class finishes, while the cached Spring
 * context keeps pointing at the dead mapped port — every later IT class then fails with
 * "connection refused".
 *
 * <p>Each test starts from an empty {@code food_logs} table, so classes can share dates without
 * polluting one another.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE food_logs, user_goal");
    }
}
