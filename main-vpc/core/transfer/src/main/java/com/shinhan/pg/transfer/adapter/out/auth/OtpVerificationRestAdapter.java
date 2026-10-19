package com.shinhan.pg.transfer.adapter.out.auth;

import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.OtpVerificationPort;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.UUID;
import java.math.BigDecimal;

/** Auth owns proof validation/consumption. No HMAC secret or OTP code is shared with Transfer. */
public final class OtpVerificationRestAdapter implements OtpVerificationPort {
    private final RestClient client;
    private final String serviceToken;
    public OtpVerificationRestAdapter(Environment env) {
        serviceToken = env.getProperty("pg.internal.service-token", "");
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(env.getProperty("pg.clients.auth.base-url", "http://localhost:8081"))
            .requestFactory(factory).build();
    }
    @Override public void verify(RequestTransferCommand command) {
        if (command.otpToken() == null || command.otpToken().isBlank()) throw invalid();
        if (serviceToken.isBlank()) throw unavailable();
        try {
            var response = client.post().uri("/internal/v1/auth/otp/consume")
                .header("X-Internal-Caller", "transfer").header("X-Internal-Service-Token", serviceToken)
                .body(new ConsumeRequest(command.actorUserId(), command.otpToken(), command.idempotencyKey(),
                    command.fromAccountId(), command.toAccountId(), command.amount(), command.currency()))
                .retrieve().toBodilessEntity();
            if (response.getStatusCode().value() != 204) throw unavailable();
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401) throw invalid();
            throw unavailable();
        } catch (RestClientException error) { throw unavailable(); }
    }
    private static TransferException invalid() { return new TransferException(TransferErrorCode.OTP_INVALID, "Invalid or consumed OTP authorization"); }
    private static TransferException unavailable() { return new TransferException(TransferErrorCode.AUTH_UNAVAILABLE, "OTP authority unavailable"); }
    private record ConsumeRequest(UUID actorUserId, String otpToken, String idempotencyKey, UUID fromAccountId,
            UUID toAccountId, BigDecimal amount, String currency) {
        @Override public String toString() { return "ConsumeRequest[REDACTED]"; }
    }
}
