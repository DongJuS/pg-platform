package com.shinhan.pg.apigateway.config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonerror.ErrorResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
/**
 * CMN-F02 접근 통제 계약:
 * - /actuator/health/**, /api/v1/auth/** → permitAll
 * - 그 외 /api/v1/** → authenticated (JWT 검증은 리소스서버가 담당 — GatewayJwtConfiguration의 decoder)
 * - 나머지(/internal/**, health 외 /actuator/**, catch-all) → denyAll
 * - 차단 응답은 기존 401/403 JSON ErrorResponse 계약(COMMON-HTTP-401/403)을 유지한다.
 *   decoder가 없으면(테스트 등) 리소스서버 필터를 등록하지 않고 인가 규칙만 적용한다.
 * - 무상태 JWT bearer API이므로 CSRF는 비활성화한다(세션 쿠키 인증 없음).
 */
@Configuration(proxyBeanMethods=false)
public class GatewaySecurityConfiguration {
    @Bean SecurityWebFilterChain security(ServerHttpSecurity http,ObjectMapper mapper,ObjectProvider<ReactiveJwtDecoder> decoderProvider) {
        ServerAuthenticationEntryPoint entryPoint=(exchange,error) -> deny(exchange,mapper,HttpStatus.UNAUTHORIZED);
        http.csrf(csrf -> csrf.disable())
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/health/**").permitAll()
                .pathMatchers("/api/v1/auth/**").permitAll()
                .pathMatchers("/api/v1/**").authenticated()
                .anyExchange().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler((exchange,error) -> deny(exchange,mapper,HttpStatus.FORBIDDEN)));
        var decoder=decoderProvider.getIfAvailable();
        if (decoder!=null) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(entryPoint)
                .jwt(jwt -> jwt.jwtDecoder(decoder)));
        }
        return http.build();
    }
    private static Mono<Void> deny(ServerWebExchange exchange,ObjectMapper mapper,HttpStatus status) {
        var response=exchange.getResponse(); response.setStatusCode(status); response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            var body=new ErrorResponse("COMMON-HTTP-"+status.value(),status.getReasonPhrase(),exchange.getAttribute("traceId"));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(mapper.writeValueAsBytes(body))));
        } catch (JsonProcessingException failure) { return Mono.error(failure); }
    }
}
