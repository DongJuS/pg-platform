package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.fepadapter.application.readmodel.SwiftReceipt;

import java.util.Optional;

public interface SwiftGatewayPort {
    void send(SwiftReceipt receipt);

    Optional<String> query(String uetr);
}
