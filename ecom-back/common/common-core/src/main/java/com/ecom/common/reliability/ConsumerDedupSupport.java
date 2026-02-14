package com.ecom.common.reliability;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ConsumerDedupSupport {

    private ConsumerDedupSupport() {
    }

    public static <T extends EventConsumptionRecord> boolean markIfNew(
            String eventId,
            Predicate<String> existsById,
            Supplier<T> recordFactory,
            Consumer<T> saver) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }
        if (existsById.test(eventId)) {
            return false;
        }
        T record = recordFactory.get();
        record.setEventId(eventId);
        record.setConsumedAt(Instant.now());
        saver.accept(record);
        return true;
    }
}
