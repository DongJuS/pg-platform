package com.shinhan.pg.swift.adapter.out.fep;

import com.shinhan.pg.common.swiftmessages.Iso20022;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.port.out.FepSwiftPort;
import com.shinhan.pg.swift.application.readmodel.SwiftAcknowledgementDraft;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;
import java.util.UUID;

public final class FepSwiftRestAdapter implements FepSwiftPort {
    private final RestClient rest;

    public FepSwiftRestAdapter(Environment env) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(5000);
        rest =
                RestClient.builder()
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.fep-adapter.base-url", "http://localhost:8091"))
                        .requestFactory(f)
                        .defaultHeader("X-Internal-Caller", "swift")
                        .defaultHeader(
                                "X-Internal-Service-Token",
                                env.getProperty("pg.swift.caller-tokens.swift", ""))
                        .build();
    }

    @Override
    public UUID send(SubmitSwiftCommand c, String xml, String ref) {
        try {
            return receipt(
                    rest.post()
                            .uri("/internal/v1/fep/swift/messages")
                            .body(new Request(c.transferId(), c.uetr(), ref, xml))
                            .retrieve()
                            .body(Receipt.class));
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    @Override
    public UUID retry(RetrySwiftCommand c) {
        try {
            return receipt(
                    rest.post()
                            .uri("/internal/v1/fep/swift/messages/{uetr}/retransmissions", c.uetr())
                            .retrieve()
                            .body(Receipt.class));
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    @Override
    public Optional<String> query(String uetr) {
        try {
            var r =
                    rest.get()
                            .uri("/internal/v1/fep/swift/messages/{uetr}/result", uetr)
                            .retrieve()
                            .body(Result.class);
            return r == null ? Optional.empty() : Optional.ofNullable(r.pacs002());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return Optional.empty();
            throw SwiftProblem.unavailable();
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    @Override
    public java.util.List<String> queryAll(String uetr) {
        return queryResult(uetr).reports();
    }

    @Override
    public com.shinhan.pg.swift.application.readmodel.SwiftQueryResult queryResult(String uetr) {
        try {
            var r =
                    rest.get()
                            .uri("/internal/v1/fep/swift/messages/{uetr}/result", uetr)
                            .retrieve()
                            .body(Result.class);
            java.util.List<String> reports =
                    r == null
                            ? java.util.List.of()
                            : r.reports() == null
                                    ? (r.pacs002() == null
                                            ? java.util.List.of()
                                            : java.util.List.of(r.pacs002()))
                                    : r.reports();
            return new com.shinhan.pg.swift.application.readmodel.SwiftQueryResult(
                    reports, r != null && "UNKNOWN".equals(r.transmissionStatus()));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404)
                return new com.shinhan.pg.swift.application.readmodel.SwiftQueryResult(
                        java.util.List.of(), true);
            throw SwiftProblem.unavailable();
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    @Override
    public Optional<SwiftAcknowledgementDraft> findAcknowledgement(String uetr) {
        return query(uetr)
                .map(
                        xml -> {
                            var r = Iso20022.result(xml);
                            return new SwiftAcknowledgementDraft(
                                    null,
                                    r.code(),
                                    "ACSC".equals(r.code()) || "RJCT".equals(r.code()));
                        });
    }

    @Override
    public UUID send(SubmitSwiftCommand c) {
        throw SwiftProblem.invalid();
    }

    private static UUID receipt(Receipt r) {
        if (r == null || r.messageId() == null || "UNKNOWN".equals(r.acceptanceStatus()))
            throw SwiftProblem.unavailable();
        return r.messageId();
    }

    private record Request(UUID transferId, String uetr, String messageKey, String pacs008) {
        @Override
        public String toString() {
            return "SwiftRequest[REDACTED]";
        }
    }

    private record Receipt(UUID messageId, String acceptanceStatus) {}

    private record Result(
            String pacs002, java.util.List<String> reports, String transmissionStatus) {
        @Override
        public String toString() {
            return "SwiftResult[REDACTED]";
        }
    }
}
