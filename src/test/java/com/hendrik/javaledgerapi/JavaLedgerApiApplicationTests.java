package com.hendrik.javaledgerapi;

import com.hendrik.javaledgerapi.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class JavaLedgerApiApplicationTests extends PostgresIntegrationTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() throws SQLException {

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            assertThat(metaData.getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }
}
