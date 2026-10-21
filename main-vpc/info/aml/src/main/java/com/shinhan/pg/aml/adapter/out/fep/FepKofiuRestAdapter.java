package com.shinhan.pg.aml.adapter.out.fep;

import com.shinhan.pg.aml.application.port.out.FepKofiuPort;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.commonerror.ErrorType;

import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

/** AML can reach only FEP; Mock institution is never called from this service. */
public final class FepKofiuRestAdapter implements FepKofiuPort {
    private final RestClient client;

    public FepKofiuRestAdapter(Environment e) {
        String base = e.getProperty("pg.clients.fep-adapter.base-url", "http://fep-adapter:8091");
        URI u = URI.create(base);
        AmlProblem.require(
                "http".equals(u.getScheme())
                        && java.util.Set.of("localhost", "127.0.0.1", "fep-adapter")
                                .contains(u.getHost())
                        && u.getUserInfo() == null
                        && u.getQuery() == null
                        && u.getFragment() == null
                        && (u.getPath().isEmpty() || "/".equals(u.getPath())));
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(2))
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(6));
        String token = e.getProperty("pg.aml.caller-tokens.aml", "");
        AmlProblem.require(!token.isBlank());
        client =
                RestClient.builder()
                        .baseUrl(base)
                        .requestFactory(factory)
                        .defaultHeader("X-Internal-Caller", "aml")
                        .defaultHeader("X-Internal-Service-Token", token)
                        .build();
    }

    public KofiuResult submit(AmlReportDocument d) {
        return call(d, "submit");
    }

    public KofiuResult query(AmlReportDocument d) {
        return call(d, "query");
    }

    public KofiuResult retransmit(AmlReportDocument d) {
        return call(d, "retry");
    }

    private KofiuResult call(AmlReportDocument d, String kind) {
        try {
            KofiuResult r =
                    switch (kind) {
                        case "submit" ->
                                client.post()
                                        .uri("/internal/v1/fep/kofiu/reports")
                                        .body(d)
                                        .retrieve()
                                        .body(KofiuResult.class);
                        case "retry" ->
                                client.post()
                                        .uri(
                                                "/internal/v1/fep/kofiu/reports/{key}/retransmissions",
                                                d.reportKey())
                                        .retrieve()
                                        .body(KofiuResult.class);
                        default ->
                                client.get()
                                        .uri("/internal/v1/fep/kofiu/reports/{key}", d.reportKey())
                                        .retrieve()
                                        .body(KofiuResult.class);
                    };
            if (r == null
                    || !r.reportId().equals(d.reportId())
                    || !r.reportKey().equals(d.reportKey())
                    || !r.contentHash().equals(d.hash())) throw AmlProblem.conflict();
            return r;
        } catch (AmlProblem e) {
            throw e;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) throw AmlProblem.missing();
            if (e.getStatusCode().value() == 409) throw AmlProblem.conflict();
            throw unavailable();
        } catch (Exception e) {
            throw unavailable();
        }
    }

    private static AmlProblem unavailable() {
        return new AmlProblem("AML-FEP-UNKNOWN", ErrorType.EXTERNAL_FAILURE);
    }
}
