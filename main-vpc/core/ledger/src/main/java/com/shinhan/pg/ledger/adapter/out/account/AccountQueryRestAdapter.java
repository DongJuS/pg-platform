package com.shinhan.pg.ledger.adapter.out.account;

import com.shinhan.pg.ledger.application.port.out.AccountQueryPort;
import com.shinhan.pg.ledger.application.readmodel.AccountAccessSnapshot;
import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorCode;
import com.shinhan.pg.common.commonerror.ErrorType;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.UUID;

/** Caller-owned internal REST client. Its token is injected at runtime and never logged. */
public final class AccountQueryRestAdapter implements AccountQueryPort {
    private final RestClient client;
    private final String token;
    public AccountQueryRestAdapter(Environment env) {
        token = env.getProperty("pg.internal.service-token", "");
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(env.getProperty("pg.clients.account.base-url", "http://localhost:8082"))
            .requestFactory(factory).build();
    }
    @Override public AccountAccessSnapshot getAccount(UUID accountId) {
        if (token.isBlank()) throw failure(ErrorType.EXTERNAL_FAILURE, "Account access verification is not configured");
        try {
            var result = client.get().uri("/internal/v1/accounts/{id}", accountId)
                .header("X-Internal-Caller", "ledger").header("X-Internal-Service-Token", token)
                .retrieve().body(AccountAccessSnapshot.class);
            if (result == null || !accountId.equals(result.accountId()) || result.ownerId() == null
                    || result.currency() == null || result.status() == null)
                throw failure(ErrorType.EXTERNAL_FAILURE, "Invalid account authority response");
            return result;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404) throw failure(ErrorType.NOT_FOUND, "Account not found");
            throw failure(ErrorType.EXTERNAL_FAILURE, "Account authority unavailable");
        } catch (RestClientException error) {
            throw failure(ErrorType.EXTERNAL_FAILURE, "Account authority unavailable");
        }
    }
    private static ApplicationException failure(ErrorType type, String message) {
        return new ApplicationException(new ErrorCode() {
            public String code() { return "ACCOUNT-ACCESS-" + type; }
            public String message() { return message; }
            public ErrorType type() { return type; }
        });
    }
}
