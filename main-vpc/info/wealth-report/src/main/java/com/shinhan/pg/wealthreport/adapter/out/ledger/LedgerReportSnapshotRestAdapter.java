package com.shinhan.pg.wealthreport.adapter.out.ledger;

import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/** Uses only the source service API. Tokens never enter report data, exception causes or logs. */
public final class LedgerReportSnapshotRestAdapter implements ReportSnapshotPort {
    private final RestClient client;
    private final String token;

    public LedgerReportSnapshotRestAdapter(Environment env) {
        token = env.getProperty("pg.internal.service-token", "");
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(10000);
        client =
                RestClient.builder()
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.ledger.base-url", "http://localhost:8083"))
                        .requestFactory(f)
                        .build();
    }

    public AssetSnapshot load(GenerateReportCommand c) {
        if (token.isBlank()) throw new ReportException(WealthReportErrorCode.SOURCE_UNAVAILABLE);
        try {
            var s =
                    client.post()
                            .uri("/internal/v1/ledger/report-snapshots")
                            .header("X-Internal-Caller", "wealth-report")
                            .header("X-Internal-Service-Token", token)
                            .body(
                                    Map.of(
                                            "userId",
                                            c.actorUserId(),
                                            "accountIds",
                                            c.accountIds(),
                                            "from",
                                            c.from(),
                                            "to",
                                            c.to(),
                                            "asOf",
                                            c.asOf()))
                            .retrieve()
                            .body(AssetSnapshot.class);
            if (s == null) throw new ReportException(WealthReportErrorCode.SNAPSHOT_INCOMPLETE);
            return s;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 403)
                throw new ReportException(WealthReportErrorCode.FORBIDDEN);
            if (e.getStatusCode().value() == 400)
                throw new ReportException(WealthReportErrorCode.INVALID_INPUT);
            throw new ReportException(WealthReportErrorCode.SOURCE_UNAVAILABLE);
        } catch (RestClientException e) {
            throw new ReportException(WealthReportErrorCode.SOURCE_UNAVAILABLE);
        }
    }
}
