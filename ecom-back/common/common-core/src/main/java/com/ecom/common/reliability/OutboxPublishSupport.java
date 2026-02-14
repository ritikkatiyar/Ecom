package com.ecom.common.reliability;

import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;

public final class OutboxPublishSupport {

    private OutboxPublishSupport() {
    }

    public static <T extends RetryableOutboxRecord> void publishPending(
            List<T> pending,
            KafkaTemplate<String, String> kafkaTemplate,
            int maxRetry,
            Consumer<T> saver) {
        for (T record : pending) {
            try {
                kafkaTemplate.send(record.getTopic(), record.getMessageKey(), record.getPayload()).get(5, TimeUnit.SECONDS);
                record.markSent();
                record.setLastError(null);
            } catch (Exception ex) {
                int attempts = record.getAttempts() + 1;
                record.setAttempts(attempts);
                if (attempts >= maxRetry) {
                    record.markFailed();
                } else {
                    record.markPending();
                }
                record.setLastError(trim(ex.getMessage(), 500));
            }
            saver.accept(record);
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
