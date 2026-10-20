package com.shinhan.pg.fepadapter.adapter.out.institutions.swift;

import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.fepadapter.application.port.out.SwiftGatewayPort;
import com.shinhan.pg.fepadapter.application.readmodel.SwiftReceipt;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;

/** Only Mock institution destinations are supported by MVP5. No real bank adapter. */
public final class SwiftGatewayRestAdapter implements SwiftGatewayPort {
    private final RestClient rest;

    public SwiftGatewayRestAdapter(Environment env) {
        String url = env.getProperty("pg.clients.mock-swift.base-url", "http://localhost:8093");
        var uri = java.net.URI.create(url);
        if (!java.util.Set.of("mock-swift", "localhost", "127.0.0.1").contains(uri.getHost())
                || !"http".equals(uri.getScheme())
                || uri.getRawUserInfo() != null)
            throw new IllegalStateException("Only local Mock SWIFT is supported");
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(4000);
        rest =
                RestClient.builder()
                        .baseUrl(url)
                        .requestFactory(f)
                        .defaultHeader("X-Internal-Caller", "fep-adapter")
                        .defaultHeader(
                                "X-Internal-Service-Token",
                                env.getProperty("pg.swift.mock-token", ""))
                        .build();
    }

    @Override
    public void send(SwiftReceipt r) {
        try {
            var response =
                    rest.post()
                            .uri("/mock/v1/swift/messages")
                            .body(new Request(r.transferId(), r.uetr(), r.xml()))
                            .retrieve()
                            .body(Receipt.class);
            if (response == null
                    || !r.uetr().equals(response.uetr())
                    || !"ACCEPTED".equals(response.status())) throw SwiftProblem.unavailable();
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    @Override
    public Optional<String> query(String uetr) {
        try {
            var result =
                    rest.get()
                            .uri("/mock/v1/swift/messages/{uetr}/result", uetr)
                            .retrieve()
                            .body(Result.class);
            return result == null ? Optional.empty() : Optional.ofNullable(result.pacs002());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return Optional.empty();
            throw SwiftProblem.unavailable();
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    private record Request(UUID transferId, String uetr, String pacs008) {
        @Override
        public String toString() {
            return "SwiftRequest[REDACTED]";
        }
    }

    private record Receipt(String uetr, String status) {}

    private record Result(String pacs002) {
        @Override
        public String toString() {
            return "SwiftResult[REDACTED]";
        }
    }
}
