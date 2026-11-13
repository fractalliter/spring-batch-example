package com.ecom.winners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.explore.JobExplorer;
import com.ecom.winners.api.DashboardController;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Properties;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

    @LocalServerPort
    private int port;

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
    @Test
    @DisplayName("Dashboard API and two consecutive job runs")
    void dashboardAndRepeatedRuns(@Autowired JobOperator operator, @Autowired JobExplorer explorer) throws Exception {
        WebClient client = WebClient.create("http://localhost:" + port);
        DashboardController.Dashboard before = client.get().uri("/api/dashboard")
                .retrieve().bodyToMono(DashboardController.Dashboard.class).block();
        assertNotNull(before);
        assertEquals("selectingWeeklyWinner", before.jobName());

        for (int i = 0; i < 2; i++) {
            Properties parameters = new Properties();
            parameters.setProperty("JobID", "repeat-test-" + i + "-" + System.nanoTime());
            Long id = operator.start("selectingWeeklyWinner", parameters);
            JobExecution execution = explorer.getJobExecution(id);
            assertNotNull(execution);
            assertEquals(ExitStatus.COMPLETED, execution.getExitStatus());
            assertEquals(3, execution.getStepExecutions().size());
            assertEquals(1, execution.getStepExecutions().stream()
                    .filter(step -> step.getStepName().equals("selectLuckyWinner"))
                    .findFirst().orElseThrow().getWriteCount());
        }

        DashboardController.Dashboard after = client.get().uri("/api/dashboard")
                .retrieve().bodyToMono(DashboardController.Dashboard.class).block();
        assertNotNull(after);
        assertEquals(10, after.usersCount());
        assertTrue(after.executions().size() >= 2);
        DashboardController.StartResult started = client.post().uri("/api/jobs/executions")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .retrieve().bodyToMono(DashboardController.StartResult.class).block();
        assertNotNull(started);
        assertNotNull(explorer.getJobExecution(started.executionId()));
    }

}
