package com.shinhan.pg.fepadapter.adapter.out.institutions.pg;

import com.shinhan.pg.fepadapter.application.port.out.PgProviderPort;
import com.shinhan.pg.fepadapter.domain.model.PgRequest;
import com.shinhan.pg.fepadapter.domain.model.PgResult;

import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Only FEP crosses the Mock PG network boundary. Financial POST is never retried. */
public final class PgProviderRestAdapter implements PgProviderPort {
    private final RestClient client;
    private final String token;
    private final io.micrometer.core.instrument.MeterRegistry registry;

    public PgProviderRestAdapter(Environment env) {
        this(env, null, io.micrometer.observation.ObservationRegistry.NOOP);
    }

    public PgProviderRestAdapter(
            Environment env,
            io.micrometer.core.instrument.MeterRegistry registry,
            io.micrometer.observation.ObservationRegistry observation) {
        this.registry = registry;
        token = env.getProperty("pg.provider.token", "");
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
        factory.setReadTimeout(3000);
        client =
                RestClient.builder()
                        .observationRegistry(observation)
                        .baseUrl(
                                env.getProperty(
                                        "pg.clients.mock-pg.base-url", "http://localhost:8092"))
                        .requestFactory(factory)
                        .build();
    }

    @Override
    public PgResult submit(PgRequest request) {
        configured();
        long start = System.nanoTime();
        boolean success = false;
        try {
            var result =
                    client.post()
                            .uri("/mock/v1/pg/operations")
                            .header("X-Mock-PG-Token", token)
                            .body(request)
                            .retrieve()
                            .body(PgResult.class);
            success = result != null;
            return result;
        } catch (RestClientException unavailable) {
            throw new IllegalStateException("PG outcome unknown");
        } finally {
            if (registry != null)
                io.micrometer.core.instrument.Timer.builder("pg.fep.adapter.send.duration")
                        .tag("result", success ? "success" : "unknown")
                        .publishPercentileHistogram()
                        .register(registry)
                        .record(
                                System.nanoTime() - start,
                                java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    @Override
    public Optional<PgResult> query(UUID id) {
        configured();
        try {
            return Optional.ofNullable(
                    client.get()
                            .uri("/mock/v1/pg/operations/{id}", id)
                            .header("X-Mock-PG-Token", token)
                            .retrieve()
                            .body(PgResult.class));
        } catch (RestClientResponseException failure) {
            if (failure.getStatusCode().value() == 404) return Optional.empty();
            throw new IllegalStateException("PG query unavailable");
        } catch (RestClientException failure) {
            throw new IllegalStateException("PG query unavailable");
        }
    }

    @Override
    public List<PgResult> results(LocalDate date) {
        configured();
        try {
            var r =
                    client.get()
                            .uri("/mock/v1/pg/results?businessDate={date}", date)
                            .header("X-Mock-PG-Token", token)
                            .retrieve()
                            .body(PgResult[].class);
            if (r == null) throw new IllegalStateException("PG file unavailable");
            return Arrays.asList(r);
        } catch (RestClientException failure) {
            throw new IllegalStateException("PG file unavailable");
        }
    }

    private void configured() {
        if (token.isBlank()) throw new IllegalStateException("PG authentication not configured");
    }
}
