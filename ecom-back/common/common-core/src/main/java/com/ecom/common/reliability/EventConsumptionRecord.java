package com.ecom.common.reliability;

import java.time.Instant;

public interface EventConsumptionRecord {

    void setEventId(String eventId);

    void setConsumedAt(Instant consumedAt);
}
