package com.shinhan.pg.transfer.adapter.out.ledger;

import org.springframework.core.env.Environment;

import java.time.Duration;

/**
 * [MVP1-F15,F20] transfer→ledger 내부 REST 클라이언트 설정. 호출자(transfer) 소유다.
 * 기존 configuration contract 주석(infra/docker/config/application-docker.yml의 TODO[CLIENT])대로
 * {@code pg.clients.ledger.base-url}을 바인딩한다 — {@code pg.docker.endpoints.*} 신설 금지.
 * 타임아웃 기본값은 지시서 명시값(connect 2s/read 5s)이며 프로퍼티로 재정의 가능하다.
 */
public record LedgerClientProperties(String baseUrl, Duration connectTimeout, Duration readTimeout, String internalServiceToken) {
    public LedgerClientProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        this(baseUrl, connectTimeout, readTimeout, "");
    }
    @Override public String toString() { return "LedgerClientProperties[baseUrl=" + baseUrl + ", token=REDACTED]"; }

    public static LedgerClientProperties from(Environment environment) {
        return new LedgerClientProperties(
                environment.getProperty("pg.clients.ledger.base-url", "http://localhost:8083"),
                Duration.parse(
                        environment.getProperty("pg.clients.ledger.connect-timeout", "PT2S")),
                Duration.parse(
                        environment.getProperty("pg.clients.ledger.read-timeout", "PT5S")),
                environment.getProperty("pg.internal.service-token", ""));
    }
}
