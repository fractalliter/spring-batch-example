package com.ecom.winners.api;

import com.ecom.winners.entity.Winner;
import com.ecom.winners.logging.StepLogCapture;
import com.ecom.winners.repositories.TransactionRepository;
import com.ecom.winners.repositories.UserRepository;
import com.ecom.winners.repositories.WinnerRepository;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private static final String JOB_NAME = "selectingWeeklyWinner";
    private final JobRepository explorer;
    private final JobOperator operator;
    private final UserRepository users;
    private final TransactionRepository transactions;
    private final WinnerRepository winners;
    private final StepLogCapture logCapture;
    private final String threshold;
    private final String topic;
    private final String csvPath;
    private final String source;

    public DashboardController(JobRepository explorer, JobOperator operator, UserRepository users,
                               TransactionRepository transactions, WinnerRepository winners,
                               StepLogCapture logCapture,
                               @Value("${select.winner.amount.threshold}") String threshold,
                               @Value("${spring.kafka.template.default-topic}") String topic,
                               @Value("${transaction.reader.resource.path}") String csvPath,
                               @Value("${userdata.reader.resource.host}") String source) {
        this.explorer = explorer;
        this.operator = operator;
        this.users = users;
        this.transactions = transactions;
        this.winners = winners;
        this.logCapture = logCapture;
        this.threshold = threshold;
        this.topic = topic;
        this.csvPath = csvPath;
        this.source = source;
    }

    @GetMapping("/dashboard")
    public Mono<Dashboard> dashboard() {
        return Mono.fromCallable(this::snapshot).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/winners")
    public Mono<List<WinnerView>> winners() {
        return Mono.fromCallable(() -> winners.findTop10ByOrderByWonAtDesc().stream()
                        .map(WinnerView::of).toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Stream log thời gian thực dưới dạng SSE.
     *
     * @param step nếu có, chỉ trả các dòng log thuộc step đó (vd. fetchUserData);
     *             bỏ trống để nhận toàn bộ log. Subscriber mới nhận ngay tối đa
     *             1000 dòng gần nhất trước khi nhận log trực tiếp.
     */
    @GetMapping(path = "/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> logs(@RequestParam(required = false) String step) {
        return logCapture.lines()
                .filter(line -> step == null || step.isBlank() || step.equals(line.step()))
                .map(line -> ServerSentEvent.builder(
                        line.time() + " " + line.level() + " " + line.message()).build());
    }

    @PostMapping(path = "/jobs/executions", consumes = "application/json")
    public Mono<StartResult> start() {
        return Mono.fromCallable(() -> {
            if (!explorer.findRunningJobExecutions(JOB_NAME).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Job đang chạy");
            }
            Properties parameters = new Properties();
            parameters.setProperty("JobID", java.util.UUID.randomUUID().toString());
            try {
                return new StartResult(operator.start(JOB_NAME, parameters));
            } catch (Exception exception) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể chạy job", exception);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Dashboard snapshot() {
        List<Run> runs = new ArrayList<>();
        for (JobInstance instance : explorer.getJobInstances(JOB_NAME, 0, 10)) {
            for (JobExecution execution : explorer.getJobExecutions(instance)) {
                List<StepRun> steps = execution.getStepExecutions().stream()
                        .map(this::toStepRun)
                        .sorted(Comparator.comparingInt(step -> stepOrder(step.name())))
                        .toList();
                runs.add(new Run(execution.getId(), execution.getStatus().name(),
                        execution.getStartTime(), execution.getEndTime(), steps));
            }
        }
        runs.sort(Comparator.comparingLong(Run::id).reversed());
        return new Dashboard(JOB_NAME, "Chủ Nhật · 23:59", threshold, topic, csvPath, source,
                users.count(), transactions.count(),
                !explorer.findRunningJobExecutions(JOB_NAME).isEmpty(),
                runs.stream().limit(10).toList());
    }

    private StepRun toStepRun(StepExecution step) {
        return new StepRun(step.getStepName(), step.getStatus().name(),
                step.getReadCount(), step.getWriteCount(), step.getSkipCount());
    }

    private int stepOrder(String name) {
        return switch (name) {
            case "fetchUserData" -> 0;
            case "insertUserTransactions" -> 1;
            case "selectLuckyWinner" -> 2;
            default -> 3;
        };
    }

    public record Dashboard(String jobName, String schedule, String threshold, String topic,
                            String csvPath, String source, long usersCount, long transactionsCount,
                            boolean running, List<Run> executions) {}
    public record Run(long id, String status, LocalDateTime startedAt, LocalDateTime finishedAt,
                      List<StepRun> steps) {}
    public record StepRun(String name, String status, long readCount, long writeCount, long skipCount) {}
    public record StartResult(long executionId) {}

    public record WinnerView(long id, Long userId, String name, String username, String phone,
                             String website, String companyName, String city, LocalDateTime wonAt) {
        static WinnerView of(Winner winner) {
            return new WinnerView(winner.getId(), winner.getUserId(), winner.getName(),
                    winner.getUsername(), winner.getPhone(), winner.getWebsite(),
                    winner.getCompanyName(), winner.getCity(), winner.getWonAt());
        }
    }
}
