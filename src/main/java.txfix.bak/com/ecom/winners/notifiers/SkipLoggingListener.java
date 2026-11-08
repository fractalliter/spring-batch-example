package com.ecom.winners.notifiers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@Component
public class SkipLoggingListener implements SkipListener<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(SkipLoggingListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        log.warn("[SKIP-READ] {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn("[SKIP-PROCESS] item={} reason={}", item, t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.warn("[SKIP-WRITE] item={} reason={}", item, t.getMessage());
    }
}
