package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.fepadapter.application.command.SendSwiftMessageCommand;
import com.shinhan.pg.fepadapter.application.readmodel.SwiftReceipt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SwiftReceiptStore {
    SwiftReceipt register(SendSwiftMessageCommand command, String reference);

    Optional<SwiftReceipt> find(String uetr);

    boolean claim(String uetr, UUID worker, boolean resend, int maximum, long minimumSeconds);

    void sent(String uetr, UUID worker, boolean unknown);

    void result(String uetr, String xml);

    List<String> reports(String uetr);

    void purge();
}
