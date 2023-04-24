package com.ecom.winners.configurations;

import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.User;
import com.ecom.winners.repositories.UserRepository;
import com.ecom.winners.transformers.WinnerProcessor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.batch.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
public class SelectWinner {
    @Bean(name = "queryLuckyWinner")
    public JdbcCursorItemReader<User> reader(
            UserRepository userRepository,
            DataSource dataSource,
            @Value("${select.winner.amount.threshold}") float threshold
    ) {
        String totalTransactions = "SELECT t.user_id, sum(t.amount) as total_purchase FROM transactions as t " +
                "WHERE (date_part('week',t.created_at) >= date_part('week', CURRENT_TIMESTAMP - interval '1 week') and " +
                "date_part('week',t.created_at) <= date_part('week', CURRENT_TIMESTAMP)) " +
                "group by t.user_id having sum(t.amount) > %.2f";
        String totalTransactionsFormatted = String.format(totalTransactions, threshold);
        String allEligibleUsersForLastWeek = "SELECT count(*) FROM (" + totalTransactionsFormatted + ") as p";
        String luckyWinner = "SELECT ts.user_id FROM (" + totalTransactionsFormatted + ") as ts " +
                "ORDER BY random() OFFSET floor(random()*(" + allEligibleUsersForLastWeek + ")) LIMIT 1";
        return new JdbcCursorItemReaderBuilder<User>()
                .name("queryLuckyWinner")
                .dataSource(dataSource)
                .sql(luckyWinner)
                .fetchSize(1)
                .rowMapper((rs, row) -> {
                    Optional<User> user = userRepository.findByUserId(rs.getLong(1));
                    return user.orElse(null);
                })
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

    @Bean(name = "selectLuckyWinner")
    public Step selectLuckyWinner(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            KafkaItemWriter<String, UserDTO> writer,
            JdbcCursorItemReader<User> reader
    ) {
        return new StepBuilder("selectLuckyWinner", jobRepository)
                .<User, UserDTO>chunk(1, transactionManager)
                .reader(reader)
                .processor(processor())
                .writer(writer)
                .build();
    }
}
