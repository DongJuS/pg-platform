package com.shinhan.pg.fepadapter.application.port.out;

public interface PaymentCallbackPort {
    void forward(byte[] raw, String signature);
}
