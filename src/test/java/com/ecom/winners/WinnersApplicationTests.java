package com.ecom.winners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("dev")
class WinnersApplicationTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    @DisplayName("application context loads")
    void contextLoads() {
        assertNotNull(jobLauncherTestUtils);
    }

    @Test
    @DisplayName("Select weekly winner job")
    void testJob(@Autowired Job selectingWeeklyWinner) throws Exception {
        jobLauncherTestUtils.setJob(selectingWeeklyWinner);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        jobExecution.getStepExecutions()
                .forEach(stepExecution -> assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus()));
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Test
    @DisplayName("Fetch data from api")
    void testUserDataStep() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("fetchUserData");
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Test
    @DisplayName("Insertion of User transactions")
    void testUserTransactionsStep() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("insertUserTransactions");
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Test
    @DisplayName("Select winner user from database")
    void testSelectWinnerStep() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("selectLuckyWinner");
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

}
