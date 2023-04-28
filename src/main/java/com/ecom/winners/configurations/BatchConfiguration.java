package com.ecom.winners.configurations;

import com.ecom.winners.notifiers.JobCompletionNotificationListener;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

@Configuration
@AllArgsConstructor
public class BatchConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(BatchConfiguration.class);

    ApplicationContext applicationContext;
    JobLauncher jobLauncher;

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

    @Scheduled(cron = "59 23 * * * SUN")
    public void runSelectingWeeklyWinnerJob() throws Exception {
        Job job = (Job) applicationContext.getBean("selectingWeeklyWinner");
        logger.info("Job "+job.getName()+" started at :" + new Date());
        JobParameters param = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();

        JobExecution execution = jobLauncher.run(job, param);

        logger.info("Job "+job.getName()+" finished with status :" + execution.getStatus());
    }
}
