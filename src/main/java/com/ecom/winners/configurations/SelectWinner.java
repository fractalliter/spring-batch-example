package com.ecom.winners.configurations;

import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.User;
import com.ecom.winners.repositories.UserRepository;
import com.ecom.winners.transformers.WinnerProcessor;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.batch.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
@AllArgsConstructor
public class SelectWinner {

    EntityManagerFactory entityManagerFactory;
    DataSource dataSource;
    UserRepository userRepository;

    @Bean(name = "queryLuckyWinner")
    public JdbcCursorItemReader<User> reader() {
        String groupByStatement = "SELECT user_id, sum(amount) as total_purchase FROM transactions " +
                "group by user_id having sum(amount) > 100";
        String totalEligibleUsers = "SELECT count(*) FROM (" + groupByStatement + ") as p";
        String luckyWinner = "SELECT user_id FROM (" + groupByStatement + ") as t " +
                "ORDER BY floor(random()*(" + totalEligibleUsers + ")) LIMIT 1";
        return new JdbcCursorItemReaderBuilder<User>()
                .name("queryLuckyWinner")
                .rowMapper((rs, row) -> {
                    Optional<User> user = userRepository.findById(rs.getLong(1));
                    return user.orElse(null);
                })
                .dataSource(dataSource)
                .fetchSize(1)
                .sql(luckyWinner)
                .build();
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

    @Bean
    public Step selectLuckyWinner(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            KafkaItemWriter<String, UserDTO> writer
    ) {
        return new StepBuilder("writeLuckyWinner", jobRepository)
                .<User, UserDTO>chunk(1, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer)
                .build();
    }
}
