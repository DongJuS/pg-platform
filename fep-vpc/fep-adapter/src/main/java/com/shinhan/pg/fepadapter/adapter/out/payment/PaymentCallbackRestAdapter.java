package com.shinhan.pg.fepadapter.adapter.out.payment;

import com.shinhan.pg.fepadapter.application.port.out.PaymentCallbackPort;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class PaymentCallbackRestAdapter implements PaymentCallbackPort {
    private final RestClient client;
    private final String token;

    public PaymentCallbackRestAdapter(Environment env) {
        this(env, io.micrometer.observation.ObservationRegistry.NOOP);
    }

    public PaymentCallbackRestAdapter(
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
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(6000);
        client =
                RestClient.builder()
                        .observationRegistry(observation)
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.payment.base-url", "http://localhost:8085"))
                        .requestFactory(factory)
                        .build();
    }

    @Override
    public void forward(byte[] raw, String signature) {
        if (token.isBlank())
            throw new IllegalStateException("Payment authentication not configured");
        try {
            client.post()
                    .uri("/internal/v1/payments/pg/webhooks")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .header("X-Internal-Caller", "fep-adapter")
                    .header("X-Internal-Service-Token", token)
                    .header("X-PG-Signature", signature)
                    .body(raw)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException failure) {
            throw new IllegalStateException("Payment callback not confirmed");
        }
    }
}
