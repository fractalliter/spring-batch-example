package com.ecom.winners.configurations;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfiguration {
    @Bean(name = "topic")
    public NewTopic topic(
            @Value("${spring.kafka.template.default-topic}") String weeklyWinnerTopic,
            @Value("${spring.kafka.winner.topic.partitions}") int partitions,
            @Value("${spring.kafka.winner.topic.replications}") int replications
    ) {
        return TopicBuilder.name(weeklyWinnerTopic)
                .partitions(partitions)
                .replicas(replications)
                .build();
    }
}
