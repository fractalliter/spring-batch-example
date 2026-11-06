package com.ecom.winners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests cho Spring Batch job.
 *
 * <p>Môi trường test sử dụng:
 * <ul>
 *   <li><b>H2 in-memory</b> thay PostgreSQL — cấu hình qua {@code application-test.properties}</li>
 *   <li><b>EmbeddedKafka</b> thay Kafka thật — broker được khởi động trong process</li>
 * </ul>
 * Không cần Docker, không cần PostgreSQL hay Kafka chạy sẵn.
 * </p>
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
// EmbeddedKafka: broker KRaft nhúng trong JVM (Kafka 4.x, Spring Boot 4)
// Không đặt brokerProperties thủ công — để KafkaClusterTestKit tự cấu hình
@EmbeddedKafka(
        partitions = 1,
        topics = {"lucky_winner"}
)
class WinnersApplicationTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    @DisplayName("Application context loads")
    void contextLoads() {
        assertNotNull(jobLauncherTestUtils);
    }

    @Test
    @DisplayName("Select weekly winner job — full job run")
    void testJob(@Autowired Job selectingWeeklyWinner) throws Exception {
        jobLauncherTestUtils.setJob(selectingWeeklyWinner);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        jobExecution.getStepExecutions()
                .forEach(stepExecution ->
                        assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus()));
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Test
    @DisplayName("Step: Fetch user data from API")
    void testUserDataStep() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("fetchUserData");
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Test
    @DisplayName("Step: Insert user transactions from CSV")
    void testUserTransactionsStep() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("insertUserTransactions");
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Test
    @DisplayName("Step: Select lucky winner and publish to Kafka")
    void testSelectWinnerStep() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("selectLuckyWinner");
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }
}
