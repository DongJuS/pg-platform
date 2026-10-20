package com.shinhan.pg.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * 기존 `pg.clients.<service>.base-url` 계약 바인딩 (infra/docker/config/application-docker.yml의
 * TODO[CLIENT] 주석 참조 — 신규 프로퍼티 체계를 만들지 않는다). gateway는 CMN-F02 라우팅 대상인 auth/account/ledger/transfer만
 * 사용한다.
 */
@ConfigurationProperties(prefix = "pg.clients")
public record GatewayClientProperties(
        ServiceEndpoint auth,
        ServiceEndpoint account,
        ServiceEndpoint ledger,
        ServiceEndpoint transfer,
        ServiceEndpoint payment,
        ServiceEndpoint wealthReport) {

    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public GatewayClientProperties {}

    public GatewayClientProperties(
            ServiceEndpoint a,
            ServiceEndpoint b,
            ServiceEndpoint c,
            ServiceEndpoint d,
            ServiceEndpoint e) {
        this(a, b, c, d, e, new ServiceEndpoint(URI.create("http://localhost:8088")));
    }

    public record ServiceEndpoint(URI baseUrl) {}
}
