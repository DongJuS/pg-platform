package com.shinhan.pg.swift.adapter.out.transfer;

import com.shinhan.pg.common.swiftmessages.Remittance;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.swift.application.port.out.TransferInstructionPort;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

public final class TransferInstructionRestAdapter implements TransferInstructionPort {
    private final RestClient rest;

    public TransferInstructionRestAdapter(Environment env) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(5000);
        rest =
                RestClient.builder()
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.transfer.base-url", "http://localhost:8084"))
                        .requestFactory(f)
                        .defaultHeader("X-Internal-Caller", "swift")
                        .defaultHeader(
                                "X-Internal-Service-Token",
                                env.getProperty("pg.swift.caller-tokens.swift", ""))
                        .build();
    }

    @Override
    public Remittance get(UUID id) {
        try {
            var r =
                    rest.get()
                            .uri("/internal/v1/transfers/overseas/{id}/instruction", id)
                            .retrieve()
                            .body(Remittance.class);
            if (r == null || !id.equals(r.transferId())) throw SwiftProblem.unavailable();
            return r;
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }
}
