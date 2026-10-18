# API Gateway implementation files

- config/GatewayRoutesConfiguration.java: TODO for external paths and target services, priority matching of detailed Ledger paths, internal path exclusion, and connecting the synchronous report timeout.
- filter/GatewayJwtFilter.java: placeholder for RS256 verification, expiry, revocation and propagation of the trusted user context.
- filter/GatewayRateLimitFilter.java: placeholder for the Redis atomic counter, TTL, 429 and failure mode.
- filter/InternalPathBlockFilter.java: placeholder for validating the boundary between internal paths and the external catch-all.
- config/DeferredOperationsApiPlan.java: candidate comments for two deferred operations APIs.

The new files above do not register real route/filter beans. Everything except the existing health endpoint stays blocked by default. No financial Controller, domain code or Repository is placed in the Gateway.
