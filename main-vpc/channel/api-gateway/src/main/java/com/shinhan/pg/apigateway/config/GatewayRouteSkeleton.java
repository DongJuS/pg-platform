package com.shinhan.pg.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

/** 향후 라우팅 조립 자리. Bean/경로/목적지를 등록하지 않는다. */
public final class GatewayRouteSkeleton {
    public RouteLocator build(RouteLocatorBuilder builder) {
        throw new UnsupportedOperationException("SKELETON api-gateway: route definitions");
    }
}
