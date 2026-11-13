package com.ecom.winners.configurations;

import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.User;
import com.ecom.winners.notifiers.ChunkLogListener;
import com.ecom.winners.transformers.WinnerProcessor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.kafka.KafkaItemWriter;
import org.springframework.batch.infrastructure.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;

/** Selects one eligible user from transactions in the previous seven days and publishes it to Kafka. */
@Configuration
public class SelectWinner {

    private static final Logger logger = LoggerFactory.getLogger(SelectWinner.class);

    /** A new reader is created for each step execution, so each run can select a winner. */
    @Bean(name = "queryLuckyWinner")
    @StepScope
    public ItemReader<User> reader(
            EntityManagerFactory entityManagerFactory,
            @Value("${select.winner.amount.threshold}") float threshold
    ) {
        return new ItemReader<>() {

            // Flag để đảm bảo chỉ read 1 lần duy nhất trong mỗi lần chạy Step
            private boolean done = false;

            @Override
            @SuppressWarnings("unchecked")
            public User read() {
                if (done) return null;
                done = true;

                EntityManager em = EntityManagerFactoryUtils
                        .getTransactionalEntityManager(entityManagerFactory);
                if (em == null) {
                    logger.warn("No transactional EntityManager found — skipping winner selection");
                    return null;
                }

                Instant now = Instant.now();
                List<?> ids = em.createNativeQuery("""
                        SELECT u.user_id
                        FROM users u
                        JOIN transactions t ON t.user_id = u.id
                        WHERE t.created_at >= :since AND t.created_at < :now
                        GROUP BY u.user_id
                        HAVING SUM(t.amount) > :threshold
                        ORDER BY RANDOM()
                        LIMIT 1
                        """)
                        .setParameter("since", Timestamp.from(now.minus(7, ChronoUnit.DAYS)))
                        .setParameter("now", Timestamp.from(now))
                        .setParameter("threshold", java.math.BigDecimal.valueOf(threshold))
                        .getResultList();
                if (ids.isEmpty()) {
                    logger.info("No eligible users found for threshold={}", threshold);
                    return null;
                }

                // Native query returns a numeric external user ID.
                Long winnerUserId = ((Number) ids.get(0)).longValue();
                logger.info("Winner user_id selected: {}", winnerUserId);

                // Load the user entity using the external ID returned by the query.
                List<User> users = em.createQuery(
                                "SELECT u FROM User u WHERE u.userId = :userId", User.class)
                        .setParameter("userId", winnerUserId)
                        .getResultList();

                if (users.isEmpty()) {
                    logger.warn("User with userId={} not found in users table", winnerUserId);
                    return null;
                }

                logger.info("Winner — chọn {} (user_id={})", users.get(0).getName(), winnerUserId);
                return users.get(0);
            }
        };
    }

    @Bean(name = "winnerProcessor")
    public WinnerProcessor processor() {
        return new WinnerProcessor();
    }

    @Bean(name = "winnerWriter")
    public KafkaItemWriter<String, UserDTO> writer(KafkaTemplate<String, UserDTO> kafkaTemplate) {
        return new KafkaItemWriterBuilder<String, UserDTO>()
                .kafkaTemplate(kafkaTemplate)
                .itemKeyMapper(userDTO -> String.valueOf(userDTO.hashCode()))
                .build();
    }

    /**
     * Step chọn người chiến thắng.
     *
     * <p>Đọc User từ JPA reader, xử lý qua {@link WinnerProcessor} → UserDTO,
     * ghi kết quả lên Kafka.</p>
     */
    @Bean(name = "selectLuckyWinner")
    public Step selectLuckyWinner(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            KafkaItemWriter<String, UserDTO> writer,
            ItemReader<User> reader,
            WinnerProcessor winnerProcessor,
            ChunkLogListener chunkLogListener
    ) {
        return new StepBuilder("selectLuckyWinner", jobRepository)
                .<User, UserDTO>chunk(10).transactionManager(transactionManager)
                .reader(reader)
                .processor(winnerProcessor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .listener(chunkLogListener)
                .build();
    }
}
