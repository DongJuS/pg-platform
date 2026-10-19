package com.shinhan.pg.transfer.adapter.out.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.LedgerOutcomeUnknownException;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * [WO-A6] transfer→ledger 내부 REST 클라이언트의 wire 계약·실패 분류를 검증한다.
 * JDK 내장 HttpServer 스텁 사용(외부 mock 서버 의존 없음).
 */
class LedgerPostingRestAdapterTest {
    private static final UUID TRANSACTION_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID JOURNAL_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID EVENT_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private LedgerPostingRestAdapter startAdapter(
            int status, String body, Duration readTimeout, AtomicReference<String> captured)
            throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    try {
                        var requestBody =
                                new String(
                                        exchange.getRequestBody().readAllBytes(),
                                        StandardCharsets.UTF_8);
                        captured.set(
                                exchange.getRequestMethod()
                                        + " "
                                        + exchange.getRequestURI()
                                        + "\n"
                                        + exchange.getRequestHeaders().getFirst("X-Internal-Caller")
                                        + "\n"
                                        + requestBody);
                        if (status < 0) {
                            Thread.sleep(2_000); // read timeout 유도
                            return;
                        }
                        var bytes = body.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(status, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } catch (Exception ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        var url = "http://127.0.0.1:" + server.getAddress().getPort();
        return new LedgerPostingRestAdapter(
                new LedgerClientProperties(url, Duration.ofSeconds(1), readTimeout));
    }

    private RequestTransferCommand command() {
        return new RequestTransferCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.fromString("30000000-0000-0000-0000-000000000010"),
                UUID.fromString("30000000-0000-0000-0000-000000000011"),
                new BigDecimal("1000"),
                "KRW",
                null,
                "key-1");
    }

    @Test
    void postMapsWireContractAndReceipt() throws Exception {
        var captured = new AtomicReference<String>();
        var adapter =
                startAdapter(
                        200,
                        "{\"transactionId\":\"" + TRANSACTION_ID + "\",\"journalId\":\""
                                + JOURNAL_ID + "\",\"eventId\":\"" + EVENT_ID + "\"}",
                        Duration.ofSeconds(5),
                        captured);

        var receipt = adapter.post(TRANSACTION_ID, command());

        assertEquals(TRANSACTION_ID, receipt.transactionId());
        assertEquals(JOURNAL_ID, receipt.journalId());
        assertEquals(EVENT_ID, receipt.eventId());
        var request = captured.get();
        assertTrue(request.startsWith("POST /internal/v1/ledger/postings"));
        assertTrue(request.lines().anyMatch(line -> line.equals("transfer")),
                "X-Internal-Caller: transfer 표식");
        assertTrue(request.contains("\"transactionId\":\"" + TRANSACTION_ID + "\""));
        // 출금 계좌는 음수, 입금 계좌는 양수 — 두 다리의 부호가 반대다.
        assertTrue(request.contains("\"signedAmount\":-1000"));
        assertTrue(request.contains("\"signedAmount\":1000"));
    }

    @Test
    void post422IsDefinitiveRejection() throws Exception {
        var adapter =
                startAdapter(
                        422,
                        "{\"code\":\"LEDGER-INSUFFICIENT-FUNDS\"}",
                        Duration.ofSeconds(5),
                        new AtomicReference<>());

        var error =
                assertThrows(
                        TransferException.class, () -> adapter.post(TRANSACTION_ID, command()));
        assertEquals(TransferErrorCode.INSUFFICIENT_FUNDS, error.code());
    }

    @Test
    void post5xxIsOutcomeUnknownNeverRepost() throws Exception {
        var adapter =
                startAdapter(
                        500,
                        "{\"code\":\"COMMON-INTERNAL\"}",
                        Duration.ofSeconds(5),
                        new AtomicReference<>());

        assertThrows(
                LedgerOutcomeUnknownException.class, () -> adapter.post(TRANSACTION_ID, command()));
    }

    @Test
    void readTimeoutIsOutcomeUnknown() throws Exception {
        var adapter =
                startAdapter(-1, "", Duration.ofMillis(200), new AtomicReference<>());

        assertThrows(
                LedgerOutcomeUnknownException.class, () -> adapter.post(TRANSACTION_ID, command()));
    }

    @Test
    void findJournalMaps404ToEmpty() throws Exception {
        var captured = new AtomicReference<String>();
        var adapter =
                startAdapter(404, "{\"code\":\"LEDGER-NOT-FOUND\"}", Duration.ofSeconds(5), captured);

        assertTrue(adapter.findJournal(TRANSACTION_ID).isEmpty());
        assertTrue(captured.get().startsWith("GET /internal/v1/ledger/transactions/" + TRANSACTION_ID));
    }

    @Test
    void findJournalMapsHitToReceipt() throws Exception {
        var adapter =
                startAdapter(
                        200,
                        "{\"transactionId\":\"" + TRANSACTION_ID + "\",\"journalId\":\""
                                + JOURNAL_ID + "\",\"eventId\":\"" + EVENT_ID
                                + "\",\"status\":\"POSTED\"}",
                        Duration.ofSeconds(5),
                        new AtomicReference<>());

        var receipt = adapter.findJournal(TRANSACTION_ID).orElseThrow();
        assertEquals(JOURNAL_ID, receipt.journalId());
        assertEquals(EVENT_ID, receipt.eventId());
    }

    @Test
    void findJournal5xxIsOutcomeUnknown() throws Exception {
        var adapter =
                startAdapter(
                        500,
                        "{\"code\":\"COMMON-INTERNAL\"}",
                        Duration.ofSeconds(5),
                        new AtomicReference<>());

        assertThrows(
                LedgerOutcomeUnknownException.class, () -> adapter.findJournal(TRANSACTION_ID));
    }
}
