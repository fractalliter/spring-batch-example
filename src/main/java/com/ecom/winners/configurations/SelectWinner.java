package com.ecom.winners.configurations;

import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.User;
import com.ecom.winners.transformers.WinnerProcessor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.repository.JobRepository;
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

/**
 * SelectWinner configuration class.
 *
 * <p>Chọn ngẫu nhiên người chiến thắng từ danh sách user có tổng giao dịch
 * trong tuần vượt ngưỡng threshold. Sử dụng JPA {@link EntityManager} thay
 * cho JDBC trực tiếp. Query là native SQL vì cần hàm PostgreSQL-specific
 * ({@code date_part}, {@code random()}).</p>
 *
 * <p>Luồng xử lý:
 * <ol>
 *   <li>Native query (qua EntityManager) → lấy {@code user_id} người chiến thắng</li>
 *   <li>JPQL query → load full {@link User} entity cùng các quan hệ</li>
 *   <li>{@link WinnerProcessor} → map sang {@link UserDTO}</li>
 *   <li>{@link KafkaItemWriter} → publish lên Kafka topic</li>
 * </ol>
 * </p>
 */
@Configuration
public class SelectWinner {

    private static final Logger logger = LoggerFactory.getLogger(SelectWinner.class);

    /**
     * ItemReader chọn user chiến thắng bằng JPA EntityManager.
     *
     * <p>Thực hiện 2 bước trong 1 lần read:
     * <ol>
     *   <li>Native SQL: tìm {@code user_id} có tổng giao dịch tuần trước > threshold,
     *       chọn ngẫu nhiên 1 người.</li>
     *   <li>JPQL: load toàn bộ {@link User} entity (bao gồm Address, Company, GeoLocation)
     *       theo {@code userId} vừa tìm được.</li>
     * </ol>
     * </p>
     *
     * <p>Reader này chỉ trả về tối đa 1 phần tử (1 người chiến thắng) rồi trả
     * {@code null} để báo kết thúc — hành vi chuẩn của Spring Batch ItemReader.</p>
     *
     * @param entityManagerFactory factory JPA được inject bởi Spring
     * @param threshold            ngưỡng tổng giao dịch tối thiểu (từ properties)
     * @return ItemReader trả về User chiến thắng hoặc null nếu không có ai đủ điều kiện
     */
    @Bean(name = "queryLuckyWinner")
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

                // ----------------------------------------------------------------
                // Bước 1: Tìm user_id chiến thắng bằng native SQL
                // Dùng EXTRACT(WEEK FROM ...) — chuẩn ANSI SQL, tương thích
                // PostgreSQL và H2.
                // RAND() tương thích H2; PostgreSQL cũng hỗ trợ RAND() từ v14+.
                // ----------------------------------------------------------------
                String nativeQuery = String.format("""
                        SELECT ts.user_id
                        FROM (
                            SELECT t.user_id
                            FROM transactions t
                            WHERE EXTRACT(WEEK FROM t.created_at) >= EXTRACT(WEEK FROM CURRENT_TIMESTAMP - INTERVAL '7' DAY)
                              AND EXTRACT(WEEK FROM t.created_at) <= EXTRACT(WEEK FROM CURRENT_TIMESTAMP)
                            GROUP BY t.user_id
                            HAVING SUM(t.amount) > %.2f
                        ) AS ts
                        ORDER BY RAND()
                        LIMIT 1
                        """, threshold);

                List<?> ids = em.createNativeQuery(nativeQuery).getResultList();
                if (ids.isEmpty()) {
                    logger.info("No eligible users found for threshold={}", threshold);
                    return null;
                }

                // PostgreSQL trả về bigint → Number, cần cast sang Long
                Long winnerUserId = ((Number) ids.get(0)).longValue();
                logger.info("Winner user_id selected: {}", winnerUserId);

                // ----------------------------------------------------------------
                // Bước 2: Load full User entity qua JPQL (eager fetch Address, Company)
                // ----------------------------------------------------------------
                List<User> users = em.createQuery(
                                "SELECT u FROM User u WHERE u.userId = :userId", User.class)
                        .setParameter("userId", winnerUserId)
                        .getResultList();

                if (users.isEmpty()) {
                    logger.warn("User with userId={} not found in users table", winnerUserId);
                    return null;
                }

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
            WinnerProcessor winnerProcessor
    ) {
        return new StepBuilder("selectLuckyWinner", jobRepository)
                .<User, UserDTO>chunk(10, transactionManager)
                .reader(reader)
                .processor(winnerProcessor)
                .writer(writer)
                .build();
    }
}
