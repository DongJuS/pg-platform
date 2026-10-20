package com.shinhan.pg.fepadapter.application.service;

import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.swiftmessages.Iso20022;
import com.shinhan.pg.common.swiftmessages.SwiftIds;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.fepadapter.application.command.SendSwiftMessageCommand;
import com.shinhan.pg.fepadapter.application.port.out.SwiftGatewayPort;
import com.shinhan.pg.fepadapter.application.port.out.SwiftReceiptStore;
import com.shinhan.pg.fepadapter.application.result.SendSwiftMessageResult;

import java.util.List;
import java.util.UUID;

/**
 * Validate → durable receipt → HTTP outside DB transaction. FEP receipt never means financial
 * completion.
 */
public final class SwiftFepService {
    private final SwiftReceiptStore store;
    private final SwiftGatewayPort gateway;
    private final boolean retransmit;
    private final int maximum;
    private final long interval;

    public SwiftFepService(
            SwiftReceiptStore store,
            SwiftGatewayPort gateway,
            boolean retransmit,
            int maximum,
            long interval) {
        this.store = store;
        this.gateway = gateway;
        this.retransmit = retransmit;
        this.maximum = maximum;
        this.interval = interval;
        SwiftProblem.require(maximum >= 1 && maximum <= 10 && interval >= 1 && interval <= 86400);
    }

    public SendSwiftMessageResult send(SendSwiftMessageCommand c) {
        if (!"swift".equals(c.callerServiceId()))
            throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
        SwiftProblem.require(
                c.transferId() != null
                        && c.messageKey() != null
                        && c.messageKey().matches("[A-Za-z0-9_-]{1,128}"));
        SwiftIds.uetr(c.uetr());
        var parsed = Iso20022.parsePayment(c.transferId(), c.pacs008());
        if (!c.uetr().equals(parsed.uetr())) throw SwiftProblem.conflict();
        store.register(c, parsed.reference());
        dispatch(c.uetr(), false);
        var row = store.find(c.uetr()).orElseThrow();
        return new SendSwiftMessageResult(row.messageId(), row.outcome());
    }

    public SendSwiftMessageResult retry(String uetr) {
        if (!retransmit) throw new SwiftProblem("RETRANSMIT_DISABLED", ErrorType.CONFLICT);
        if (!query(uetr).isEmpty()) throw SwiftProblem.conflict();
        if (!dispatch(uetr, true)) throw SwiftProblem.conflict();
        var r = store.find(uetr).orElseThrow();
        return new SendSwiftMessageResult(r.messageId(), r.outcome());
    }

    private boolean dispatch(String uetr, boolean resend) {
        UUID worker = UUID.randomUUID();
        if (!store.claim(uetr, worker, resend, maximum, interval)) return false;
        try {
            gateway.send(store.find(uetr).orElseThrow());
            store.sent(uetr, worker, false);
        } catch (RuntimeException unknown) {
            store.sent(uetr, worker, true);
        }
        return true;
    }

    public List<String> query(String uetr) {
        SwiftIds.uetr(uetr);
        store.find(uetr).orElseThrow(() -> new SwiftProblem("NOT_FOUND", ErrorType.NOT_FOUND));
        try {
            gateway.query(uetr).ifPresent(xml -> store.result(uetr, xml));
        } catch (SwiftProblem unknown) {
            /* Absence/unavailability never authorizes a new payment. */
        }
        return store.reports(uetr);
    }

    public void callback(String uetr, String xml) {
        store.result(uetr, xml);
    }

    public void purge() {
        store.purge();
    }

    public String transmissionStatus(String uetr) {
        return store.find(uetr).orElseThrow(SwiftProblem::invalid).outcome();
    }
}
