package com.shinhan.pg.fepadapter.application.service;

import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.fepadapter.application.port.out.KofiuGatewayPort;
import com.shinhan.pg.fepadapter.application.port.out.KofiuReceiptPort;

import java.util.UUID;

/** Receipt commit precedes HTTP; duplicate submissions never trigger institution retransmission. */
public final class KofiuFepService {
    private final KofiuReceiptPort store;
    private final KofiuGatewayPort gateway;
    private final boolean retransmit;
    private final int maximum;
    private final long interval;

    public KofiuFepService(
            KofiuReceiptPort store,
            KofiuGatewayPort gateway,
            boolean retransmit,
            int maximum,
            long interval) {
        this.store = store;
        this.gateway = gateway;
        this.retransmit = retransmit;
        this.maximum = maximum;
        this.interval = interval;
        AmlProblem.require(maximum >= 1 && maximum <= 10 && interval >= 1 && interval <= 86400);
    }

    public void purgeExpired() {
        store.purgeExpired();
    }

    public KofiuResult submit(String caller, AmlReportDocument d) {
        authorize(caller);
        store.register(d);
        dispatch(d.reportKey(), false);
        return store.get(d.reportKey());
    }

    public KofiuResult query(String caller, String key) {
        authorize(caller);
        var d = store.document(key);
        try {
            store.observe(key, gateway.query(d));
        } catch (AmlProblem e) {
            if (e.type() != ErrorType.EXTERNAL_FAILURE) throw e;
        }
        return store.get(key);
    }

    public KofiuResult retry(String caller, String key) {
        authorize(caller);
        if (!retransmit) throw new AmlProblem("AML-RETRANSMIT-DISABLED", ErrorType.CONFLICT);
        // Require this lookup to succeed NOW. A previous NOT_RECEIVED snapshot cannot authorize a
        // send.
        var d = store.document(key);
        var observed = gateway.query(d);
        store.observe(key, observed);
        if (!"NOT_RECEIVED".equals(observed.institutionStatus()) || !dispatch(key, true))
            throw AmlProblem.conflict();
        return store.get(key);
    }

    private boolean dispatch(String key, boolean retry) {
        var worker = UUID.randomUUID();
        if (!store.claim(key, worker, retry, maximum, interval)) return false;
        KofiuResult observed = null;
        try {
            observed = gateway.send(store.document(key));
        } catch (AmlProblem e) {
            if (e.type() != ErrorType.EXTERNAL_FAILURE) {
                store.finish(key, worker, null);
                throw e;
            }
        }
        store.finish(key, worker, observed);
        return true;
    }

    private static void authorize(String caller) {
        if (!"aml".equals(caller)) throw new AmlProblem("AML-FEP-FORBIDDEN", ErrorType.FORBIDDEN);
    }
}
