package com.shinhan.pg.payment.adapter.out.account;

import com.shinhan.pg.payment.application.port.out.AccountAuthorityPort;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

public final class AccountAuthorityRestAdapter implements AccountAuthorityPort {
    private final RestClient client;
    private final String token;

    public AccountAuthorityRestAdapter(Environment env) {
        this(env, io.micrometer.observation.ObservationRegistry.NOOP);
    }

    public AccountAuthorityRestAdapter(
            Environment env, io.micrometer.observation.ObservationRegistry observation) {
        token = env.getProperty("pg.internal.service-token", "");
        var factory =
                new SimpleClientHttpRequestFactory() {
                    @Override
                    protected void prepareConnection(
                            java.net.HttpURLConnection connection, String method)
                            throws java.io.IOException {
                        super.prepareConnection(connection, method);
                        connection.setInstanceFollowRedirects(false);
                    }
                };
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client =
                RestClient.builder()
                        .observationRegistry(observation)
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.account.base-url", "http://localhost:8082"))
                        .requestFactory(factory)
                        .build();
    }

    public record Account(UUID accountId, UUID ownerId, String currency, String status) {}

    private Account get(UUID id) {
        if (token.isBlank()) throw new PaymentException(PaymentErrorCode.INVALID_STATE);
        try {
            var a =
                    client.get()
                            .uri("/internal/v1/accounts/{id}", id)
                            .header("X-Internal-Caller", "payment")
                            .header("X-Internal-Service-Token", token)
                            .retrieve()
                            .body(Account.class);
            if (a == null || !id.equals(a.accountId()) || a.ownerId() == null)
                throw new PaymentException(PaymentErrorCode.INVALID_STATE);
            return a;
        } catch (RestClientResponseException failure) {
            throw new PaymentException(
                    failure.getStatusCode().value() == 404
                            ? PaymentErrorCode.NOT_FOUND
                            : PaymentErrorCode.INVALID_STATE);
        } catch (RestClientException failure) {
            throw new PaymentException(PaymentErrorCode.INVALID_STATE);
        }
    }

    @Override
    public void verify(UUID actor, UUID source, UUID destination, String currency) {
        var from = get(source);
        var to = get(destination);
        if (!actor.equals(from.ownerId())) throw new PaymentException(PaymentErrorCode.FORBIDDEN);
        if (!currency.equals(from.currency())
                || !currency.equals(to.currency())
                || !"ACTIVE".equals(from.status())
                || !"ACTIVE".equals(to.status()))
            throw new PaymentException(PaymentErrorCode.INVALID_STATE);
    }
}
