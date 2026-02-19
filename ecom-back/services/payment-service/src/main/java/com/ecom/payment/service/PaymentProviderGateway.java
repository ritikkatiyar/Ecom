package com.ecom.payment.service;

import java.math.BigDecimal;

public interface PaymentProviderGateway {

    String createPaymentId(String orderId, BigDecimal amount, String currency);

    void setOutageMode(boolean enabled);

    boolean isOutageMode();
}
