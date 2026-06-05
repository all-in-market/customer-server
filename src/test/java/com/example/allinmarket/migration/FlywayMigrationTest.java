package com.example.allinmarket.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

    private static final DockerImageName PGVECTOR_POSTGRES_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PGVECTOR_POSTGRES_IMAGE)
            .withDatabaseName("allinmarket")
            .withUsername("postgres")
            .withPassword("postgres");

    @Test
    void flywayMigrationsCreatePostgresSpecificIndexesAndExtensions() throws Exception {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            assertThat(indexExists(connection, "uq_payments_order_success")).isTrue();
            assertThat(indexExists(connection, "ux_address_default")).isTrue();
            assertThat(indexExists(connection, "idx_dashboard_outbox_polling")).isTrue();
            assertThat(indexExists(connection, "idx_history_outboxes_polling")).isTrue();
            assertThat(indexExists(connection, "idx_langchain4j_embedding_store_text_trgm")).isTrue();
            assertThat(extensionExists(connection, "vector")).isTrue();
            assertThat(extensionExists(connection, "pg_trgm")).isTrue();
        }
    }

    private boolean indexExists(Connection connection, String indexName) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_indexes
                    WHERE schemaname = 'public'
                      AND indexname = ?
                )
                """)) {
            statement.setString(1, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private boolean extensionExists(Connection connection, String extensionName) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_extension
                    WHERE extname = ?
                )
                """)) {
            statement.setString(1, extensionName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }
}
