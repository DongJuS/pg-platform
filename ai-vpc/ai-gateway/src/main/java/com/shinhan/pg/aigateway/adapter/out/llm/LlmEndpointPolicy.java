package com.shinhan.pg.aigateway.adapter.out.llm;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** 외부 client에 적용할 정확한 hostname 허용 정책. DNS/IP 제약은 별도 네트워크 규칙으로 보완한다. */
public final class LlmEndpointPolicy {
    private final Set<String> allowedHosts;

    public LlmEndpointPolicy(Set<String> allowedHosts) {
        this.allowedHosts =
                allowedHosts.stream()
                        .map(host -> host.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet());
    }

    public void requireAllowed(URI endpoint) {
        if (endpoint == null
                || !"https".equalsIgnoreCase(endpoint.getScheme())
                || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null
                || endpoint.getRawFragment() != null
                || (endpoint.getPort() != -1 && endpoint.getPort() != 443)
                || !allowedHosts.contains(endpoint.getHost().toLowerCase(Locale.ROOT)))
            throw new IllegalArgumentException(
                    "LLM endpoint must use HTTPS:443 and an explicitly allowed hostname");
    }
}
