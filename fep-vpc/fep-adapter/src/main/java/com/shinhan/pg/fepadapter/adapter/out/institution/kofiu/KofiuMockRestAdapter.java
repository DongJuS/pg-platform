package com.shinhan.pg.fepadapter.adapter.out.institution.kofiu;

import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.fepadapter.application.port.out.KofiuGatewayPort;

import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

public final class KofiuMockRestAdapter implements KofiuGatewayPort {
    private final RestClient client;

    public KofiuMockRestAdapter(Environment e) {
        String base = e.getProperty("pg.clients.mock-kofiu.base-url", "http://mock-kofiu:8094");
        URI uri = URI.create(base);
        AmlProblem.require(
                "http".equals(uri.getScheme())
                        && java.util.Set.of("localhost", "127.0.0.1", "mock-kofiu")
                                .contains(uri.getHost())
                        && uri.getUserInfo() == null
                        && uri.getQuery() == null
                        && uri.getFragment() == null
                        && (uri.getPath().isEmpty() || "/".equals(uri.getPath())));
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(2))
                                .followRedirects(HttpClient.Redirect.NEVER)
                                .build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        String token = e.getProperty("pg.aml.mock-token", "");
        AmlProblem.require(!token.isBlank());
        client =
                RestClient.builder()
                        .baseUrl(base)
                        .requestFactory(factory)
                        .defaultHeader("X-Kofiu-Token", token)
                        .build();
    }

    public KofiuResult send(AmlReportDocument d) {
        try {
            return checked(
                    d,
                    client.post()
                            .uri("/mock/v1/kofiu/reports")
                            .body(d)
                            .retrieve()
                            .body(KofiuResult.class));
        } catch (AmlProblem e) {
            throw e;
        } catch (Exception e) {
            throw unavailable();
        }
    }

    public KofiuResult query(AmlReportDocument d) {
        try {
            return checked(
                    d,
                    client.post()
                            .uri("/mock/v1/kofiu/report-queries")
                            .body(new Query(d.reportId(), d.reportKey(), d.hash()))
                            .retrieve()
                            .body(KofiuResult.class));
        } catch (AmlProblem e) {
            throw e;
        } catch (Exception e) {
            throw unavailable();
        }
    }

    private KofiuResult checked(AmlReportDocument d, KofiuResult r) {
        if (r == null
                || !r.reportId().equals(d.reportId())
                || !r.reportKey().equals(d.reportKey())
                || !r.contentHash().equals(d.hash())) throw AmlProblem.conflict();
        return r;
    }

    private static AmlProblem unavailable() {
        return new AmlProblem("AML-INSTITUTION-UNKNOWN", ErrorType.EXTERNAL_FAILURE);
    }

    public record Query(java.util.UUID reportId, String reportKey, String contentHash) {}
}
