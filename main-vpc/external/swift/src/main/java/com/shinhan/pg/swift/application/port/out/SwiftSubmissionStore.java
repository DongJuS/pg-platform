package com.shinhan.pg.swift.application.port.out;

import com.shinhan.pg.common.swiftmessages.Iso20022;
import com.shinhan.pg.common.swiftmessages.Remittance;
import com.shinhan.pg.swift.application.readmodel.StoredSwiftMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SwiftSubmissionStore {
    StoredSwiftMessage register(Remittance r, String mt103, String xml);

    Optional<StoredSwiftMessage> find(String uetr);

    boolean claim(String uetr, UUID worker, boolean resend, int maximum, long minimumSeconds);

    void sent(String uetr, UUID worker, boolean unknown);

    void transmissionUnknown(String uetr);

    void result(String uetr, String xml, Iso20022.Result result);

    List<StoredSwiftMessage> due();

    void deferQuery(String uetr);

    List<String> history(String uetr, boolean raw);

    void purge();
}
