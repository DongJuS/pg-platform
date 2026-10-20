package com.shinhan.pg.wealthreport.adapter.out.aigateway;

import com.shinhan.pg.wealthreport.application.port.out.AiPreparationPort;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the active MVP3 contract, independently of the absent inference API. Intentionally not
 * wired into generation until inference/response inspection is authorized. Caller credential is
 * distinct from the finance token. No transport retries, body logging or restoration hints.
 */
public final class AiPreparationRestAdapter implements AiPreparationPort {
    private final RestClient client;
    private final String token;

    public AiPreparationRestAdapter(Environment env) {
        token = env.getProperty("pg.clients.ai-gateway.preparation-token", "");
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(5000);
        client =
                RestClient.builder()
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.ai-gateway.base-url", "http://localhost:8090"))
                        .requestFactory(f)
                        .build();
    }

    public record Preparation(
            UUID requestId,
            String status,
            String reason,
            String maskedPrompt,
            Map<String, Integer> maskedCounts,
            String policyVersion,
            boolean externalTransferPerformed) {}

    public void checkAggregateText(String text) {
        if (token.isBlank()) throw new ReportException(WealthReportErrorCode.AI_NOT_CONFIGURED);
        try {
            var p =
                    client.post()
                            .uri("/internal/v1/ai/preparations")
                            .header("X-Internal-Caller", "wealth-report")
                            .header("X-Internal-Service-Token", token)
                            .body(
                                    Map.of(
                                            "inputFormat",
                                            "TEXT",
                                            "prompt",
                                            text,
                                            "sensitiveValues",
                                            List.of()))
                            .retrieve()
                            .body(Preparation.class);
            if (p == null
                    || p.requestId() == null
                    || !"READY_FOR_TRANSFER".equals(p.status())
                    || !"CHECKS_PASSED".equals(p.reason())
                    || p.externalTransferPerformed()
                    || p.maskedPrompt() == null
                    || p.policyVersion() == null)
                throw new ReportException(WealthReportErrorCode.AI_RESULT_INVALID);
        } catch (RestClientException e) {
            throw new ReportException(WealthReportErrorCode.SOURCE_UNAVAILABLE);
        }
    }
}
