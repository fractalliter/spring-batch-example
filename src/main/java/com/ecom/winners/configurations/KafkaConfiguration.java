package com.ecom.winners.configurations;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfiguration {

    @Value("${spring.kafka.template.default-topic}")
    private String weeklyWinnerTopic;

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name(weeklyWinnerTopic)
                .partitions(10)
                .replicas(1)
                .build();
    }
}
