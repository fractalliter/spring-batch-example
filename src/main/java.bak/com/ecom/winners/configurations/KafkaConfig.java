package com.ecom.winners.configurations;

import com.ecom.winners.dto.UserDTO;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration: topic và KafkaTemplate với generic type rõ ràng.
 *
 * <p>Spring Boot auto-configure tạo {@code ProducerFactory<Object, Object>} không
 * phù hợp để inject làm {@code ProducerFactory<String, UserDTO>}. Vì vậy ta tạo
 * {@link ProducerFactory} và {@link KafkaTemplate} tường minh ở đây.</p>
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.client-id:winners-producer}")
    private String clientId;

    @Value("${spring.kafka.template.default-topic}")
    private String winnerTopic;

    @Value("${spring.kafka.winner.topic.partitions:1}")
    private int partitions;

    @Value("${spring.kafka.winner.topic.replications:1}")
    private short replications;

    /**
     * Tạo topic {@code lucky_winner} tự động nếu chưa tồn tại.
     */
    @Bean
    public NewTopic luckyWinnerTopic() {
        return new NewTopic(winnerTopic, partitions, replications);
    }

    /**
     * ProducerFactory với kiểu tường minh {@code <String, UserDTO>}.
     */
    @Bean
    public ProducerFactory<String, UserDTO> userDtoProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Cho phép serialize bất kỳ type nào (không giới hạn package)
        config.put("spring.json.trusted.packages", "*");
        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * KafkaTemplate với kiểu tường minh {@code <String, UserDTO>}.
     * Default topic bắt buộc phải set vì {@code KafkaItemWriterBuilder} validate điều này.
     */
    @Bean
    public KafkaTemplate<String, UserDTO> kafkaTemplate() {
        KafkaTemplate<String, UserDTO> template = new KafkaTemplate<>(userDtoProducerFactory());
        template.setDefaultTopic(winnerTopic);
        return template;
    }
}
