package com.shinhan.pg.apigateway.config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonerror.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@Configuration(proxyBeanMethods=false)
public class GatewaySecurityConfiguration {
    @Bean SecurityWebFilterChain security(ServerHttpSecurity http,ObjectMapper mapper) {
        return http.authorizeExchange(auth -> auth.pathMatchers("/actuator/health/**").permitAll().anyExchange().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((exchange,error) -> deny(exchange,mapper,HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((exchange,error) -> deny(exchange,mapper,HttpStatus.FORBIDDEN))).build();
    }
    private static Mono<Void> deny(ServerWebExchange exchange,ObjectMapper mapper,HttpStatus status) {
        var response=exchange.getResponse(); response.setStatusCode(status); response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            var body=new ErrorResponse("COMMON-HTTP-"+status.value(),status.getReasonPhrase(),exchange.getAttribute("traceId"));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(mapper.writeValueAsBytes(body))));
        } catch (JsonProcessingException failure) { return Mono.error(failure); }
    }
}
