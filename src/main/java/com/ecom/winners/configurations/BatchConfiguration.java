package com.ecom.winners.configurations;

import com.ecom.winners.notifiers.ChunkLogListener;
import com.ecom.winners.notifiers.JobCompletionNotificationListener;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khai báo Spring Batch Job bean.
 *
 * <p>Tách riêng khỏi {@link JobScheduler} để tránh circular dependency:
 * JobOperator (auto-configured) → cần Job beans → không được inject JobOperator
 * trong cùng class khai báo Job.</p>
 */
@Configuration
public class BatchConfiguration {

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public ChunkLogListener chunkLogListener() {
        return new ChunkLogListener();
    }

    @Bean
    public Job selectingWeeklyWinner(
            JobRepository jobRepository,
            JobCompletionNotificationListener listener,
            Step fetchUserData,
            Step insertUserTransactions,
            Step selectLuckyWinner
    ) {
        return new JobBuilder("selectingWeeklyWinner", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(fetchUserData)
                .next(insertUserTransactions)
                .next(selectLuckyWinner)
                .build();
    }
}
