package com.ecom.winners;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@ActiveProfiles("test")
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
        assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
    }

}
