package com.shinhan.pg.transfer.adapter.out.ledger;

import com.shinhan.pg.common.swiftmessages.SwiftIds;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.transfer.application.port.out.OverseasLedgerReservationPort;
import com.shinhan.pg.transfer.application.readmodel.OverseasSnapshot;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

public final class OverseasLedgerRestAdapter implements OverseasLedgerReservationPort {
    private final RestClient rest;

    public OverseasLedgerRestAdapter(Environment env) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(5000);
        rest =
                RestClient.builder()
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.ledger.base-url", "http://localhost:8083"))
                        .requestFactory(f)
                        .defaultHeader("X-Internal-Caller", "transfer")
                        .defaultHeader(
                                "X-Internal-Service-Token",
                                env.getProperty("pg.swift.caller-tokens.transfer", ""))
                        .build();
    }

    @Override
    public void reserve(OverseasSnapshot r) {
        if (r.remittance() == null) throw SwiftProblem.unavailable();
        try {
            var response =
                    rest.post()
                            .uri("/internal/v1/ledger/overseas-reservations")
                            .body(
                                    new Request(
                                            r.transferId(),
                                            r.uetr(),
                                            r.actorUserId(),
                                            r.fromAccountId(),
                                            r.remittance().amount(),
                                            r.remittance().currency()))
                            .retrieve()
                            .body(Receipt.class);
            if (response == null
                    || !SwiftIds.id(r.transferId(), "hold").equals(response.transactionId())
                    || response.journalId() == null) throw SwiftProblem.unavailable();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 422)
                throw new TransferException(
                        TransferErrorCode.INSUFFICIENT_FUNDS,
                        "Ledger rejected overseas reservation");
            throw SwiftProblem.unavailable();
        } catch (RestClientException e) {
            throw SwiftProblem.unavailable();
        }
    }

    private record Request(
            UUID transferId,
            String uetr,
            UUID actorUserId,
            UUID fromAccountId,
            BigDecimal amount,
            String currency) {}

    private record Receipt(UUID transactionId, UUID journalId, UUID eventId) {}
}
