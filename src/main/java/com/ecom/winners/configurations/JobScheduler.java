package com.ecom.winners.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Properties;

/**
 * Lập lịch chạy job chọn người chiến thắng mỗi Chủ Nhật lúc 23:59.
 *
 * <p>Tách khỏi {@link BatchConfiguration} để tránh circular dependency:
 * <pre>
 *   BatchConfiguration khai báo Job bean
 *       ↑ cần bởi JobOperator (auto-configure)
 *       ↑ được inject vào đây (JobScheduler)
 * </pre>
 * Nếu BatchConfiguration inject JobOperator trực tiếp → circular.
 * Tách ra class riêng phá vòng tròn này.</p>
 *
 * @see JobOperator
 */
@Configuration
public class JobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    private final JobOperator jobOperator;

    public JobScheduler(JobOperator jobOperator) {
        this.jobOperator = jobOperator;
    }

    /**
     * Trigger job mỗi Chủ Nhật 23:59:00.
     * Dùng {@link JobOperator#start(String, Properties)} — API chính trong Spring Batch 6.
     */
    @Scheduled(cron = "0 59 23 * * SUN", zone = "${APP_TIME_ZONE:Asia/Ho_Chi_Minh}")
    public void runSelectingWeeklyWinnerJob() throws Exception {
        logger.info("Triggering selectingWeeklyWinner job...");
        Properties params = new Properties();
        params.setProperty("JobID", String.valueOf(System.currentTimeMillis()));
        Long executionId = jobOperator.start("selectingWeeklyWinner", params);
        logger.info("Job selectingWeeklyWinner started — executionId: {}", executionId);
    }
}
