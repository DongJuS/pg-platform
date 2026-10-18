package com.shinhan.pg.aigateway;

import static org.junit.jupiter.api.Assertions.*;

import com.shinhan.pg.aigateway.adapter.out.llm.LlmEndpointPolicy;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

class LlmEndpointPolicyTest {
    @Test
    void emptyAllowlistKeepsOutboundDestinationsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new LlmEndpointPolicy(Set.of())
                                .requireAllowed(URI.create("https://api.example.com/v1")));
    }

    @Test
    void allowsOnlyTheExactHttpsHostnameAndStandardPort() {
        var policy = new LlmEndpointPolicy(Set.of("api.example.com"));
        assertDoesNotThrow(() -> policy.requireAllowed(URI.create("https://api.example.com/v1")));
        for (var uri :
                new String[] {
                    "http://api.example.com/v1",
                    "https://api.example.com.evil.test/v1",
                    "https://evil.test/api.example.com",
                    "https://user:secret@api.example.com/v1",
                    "https://api.example.com:8443/v1",
                    "https://api.example.com/v1#hidden"
                })
            assertThrows(
                    IllegalArgumentException.class,
                    () -> policy.requireAllowed(URI.create(uri)),
                    uri);
    }
}
