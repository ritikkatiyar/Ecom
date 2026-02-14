package com.ecom.common.reliability;

public interface RetryableOutboxRecord {

    String getTopic();

    String getMessageKey();

    String getPayload();

    int getAttempts();

    void setAttempts(Integer attempts);

    void setLastError(String lastError);

    void markSent();

    void markPending();

    void markFailed();
}
