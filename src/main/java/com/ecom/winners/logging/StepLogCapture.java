package com.ecom.winners.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chụp toàn bộ log của ứng dụng vào một stream Reactor để dashboard stream
 * theo thời gian thực qua SSE ({@code GET /api/logs/stream}).
 *
 * <p>Mỗi dòng log được gán nhãn step dựa trên các dòng marker do Spring Batch
 * in ra: {@code "Executing step: [name]"} gán step mới, {@code "Step: [name]
 * executed"} giữ nguyên nhãn cho các dòng log kết thúc step. Dòng ngoài job
 * (startup, Kafka consumer...) có nhãn rỗng.</p>
 *
 * <p>Stream dùng replay 1000 dòng để mỗi subscriber mới nhận ngay lịch sử
 * gần nhất rồi tiếp tục nhận log trực tiếp. Emit được khóa bằng synchronized
 * vì appender có thể được gọi từ nhiều thread song song.</p>
 */
@Component
public class StepLogCapture extends AppenderBase<ILoggingEvent> {

    private static final Pattern STEP_START = Pattern.compile("Executing step: \\[(.+?)\\]");
    private static final Pattern STEP_DONE = Pattern.compile("Step: \\[(.+?)\\] executed");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Sinks.Many<LogLine> stream =
            Sinks.many().replay().limit(5000);

    private volatile String currentStep = "";

    public StepLogCapture() {
        setName("step-log-capture");
        start();
    }

    /** Gắn appender này vào root logger một lần duy nhất khi bean được tạo. */
    @PostConstruct
    void attach() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        if (context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
                .getAppender(getName()) == null) {
            setContext(context);
            context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(this);
        }
    }

    /** Toàn bộ dòng log gần nhất (tối đa 1000) kèm dòng mới theo thời gian thực. */
    public Flux<LogLine> lines() {
        return stream.asFlux();
    }

    @Override
    protected void append(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        Matcher start = STEP_START.matcher(message);
        if (start.find()) {
            currentStep = start.group(1);
        } else {
            Matcher done = STEP_DONE.matcher(message);
            if (done.find()) currentStep = done.group(1);
        }
        LogLine line = new LogLine(
                LocalDateTime.now().format(TIME),
                event.getLevel().toString(),
                currentStep,
                message);
        synchronized (stream) {
            stream.tryEmitNext(line);
        }
    }

    /**
     * Một dòng log: thời điểm, mức, nhãn step (rỗng nếu ngoài job) và nội dung.
     */
    public record LogLine(String time, String level, String step, String message) {}
}
