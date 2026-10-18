package @@WEB_PACKAGE@@;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class TraceFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var previous=MDC.get("traceId");
        var traceId=previous;
        if (traceId==null) {
            var supplied=request.getHeader("X-Trace-Id");
            traceId=supplied!=null && supplied.matches("[0-9a-f]{32}") ? supplied : UUID.randomUUID().toString().replace("-","");
        }
        MDC.put("traceId",traceId);
        response.setHeader("X-Trace-Id",traceId);
        try { chain.doFilter(request,response); }
        finally { if (previous==null) MDC.remove("traceId"); else MDC.put("traceId",previous); }
    }
}
