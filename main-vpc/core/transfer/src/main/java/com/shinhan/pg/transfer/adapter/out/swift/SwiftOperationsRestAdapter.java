package com.shinhan.pg.transfer.adapter.out.swift;

import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.transfer.application.port.out.SwiftOperationsPort;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

public final class SwiftOperationsRestAdapter implements SwiftOperationsPort {
    private final RestClient client;

    public SwiftOperationsRestAdapter(Environment env) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(12000);
        client =
                RestClient.builder()
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.swift.base-url", "http://localhost:8087"))
                        .requestFactory(f)
                        .defaultHeader("X-Internal-Caller", "swift-operations")
                        .defaultHeader(
                                "X-Internal-Service-Token",
                                env.getProperty("pg.swift.caller-tokens.swift-operations", ""))
                        .build();
    }

    @Override
    public List<String> history(String uetr) {
        try {
            var r =
                    client.get()
                            .uri("/internal/v1/swift/transfers/{uetr}/messages", uetr)
                            .retrieve()
                            .body(String[].class);
            return r == null ? List.of() : List.of(r);
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    @Override
    public String query(String uetr) {
        return action(uetr, "result-queries");
    }

    @Override
    public String retransmit(String uetr) {
        return action(uetr, "retransmissions");
    }

    private String action(String uetr, String action) {
        try {
            var r =
                    client.post()
                            .uri("/internal/v1/swift/transfers/{uetr}/" + action, uetr)
                            .retrieve()
                            .body(Receipt.class);
            if (r == null) throw SwiftProblem.unavailable();
            return r.status();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) throw SwiftProblem.conflict();
            throw SwiftProblem.unavailable();
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    private record Receipt(String status) {}
}
