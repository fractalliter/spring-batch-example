package com.ecom.winners;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@EnableBatchProcessing
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
