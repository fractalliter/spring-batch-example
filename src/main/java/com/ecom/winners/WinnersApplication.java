package com.ecom.winners;

import com.zaxxer.hikari.util.IsolationLevel;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * Winner application is a Spring Batch application including a single job with three steps.
 * It uses a scheduler which runs weekly to select a lucky winner from a list of users and their
 * transactions that meets a threshold.
 * Annotations:
 *
 * @see SpringBootApplication
 * @see EnableScheduling
 * @see EnableJpaAuditing
 * @see EnableBatchProcessing
 * @see IsolationLevel
 */
@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@EnableBatchProcessing(isolationLevelForCreate = "ISOLATION_REPEATABLE_READ")
public class WinnersApplication {

    @Value("${spring.datasource.driver-class-name}")
    private String databaseDriver;
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    @Value("${spring.datasource.username}")
    private String databaseUsername;
    @Value("${spring.datasource.password}")
    private String databasePassword;

    public static void main(String[] args) {
        SpringApplication.run(WinnersApplication.class, args);
    }

    /**
     * Datasource bean is a configuration bean.
     * This bean aims to replace the data source initializer bean from JPA.
     * It can be configured for more customization of connection to data sources.
     * In this case, Postgres database.
     *
     * @return DataSource
     */
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName(databaseDriver)
                .url(databaseUrl)
                .username(databaseUsername)
                .password(databasePassword)
                .build();
    }
}
