package com.shinhan.pg.transfer.adapter.in.web;
import com.shinhan.pg.common.commonerror.*;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class PlatformWebSupportTest {
    record ProbeError(ErrorType type) implements ErrorCode {
        public String code() { return "TEST-ERROR"; }
        public String message() { return "Test request failed"; }
    }
    @RestController static class Probe {
        @GetMapping("/probe/{type}") void fail(@PathVariable String type) {
            throw new ApplicationException(new ProbeError(ErrorType.valueOf(type)));
        }
    }
    @Test void mapsAllErrorTypesAndCleansTraceMdc() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new PlatformExceptionHandler()).addFilters(new TraceFilter()).build();
        var types=ErrorType.values(); var codes=new int[]{400,404,409,401,403,502,500};
        var trace="0123456789abcdef0123456789abcdef";
        for (int i=0;i<types.length;i++) {
            mvc.perform(get("/probe/"+types[i]).header("X-Trace-Id",trace))
                .andExpect(status().is(codes[i])).andExpect(header().string("X-Trace-Id",trace))
                .andExpect(jsonPath("$.code").value("TEST-ERROR")).andExpect(jsonPath("$.traceId").value(trace));
            assertNull(MDC.get("traceId"));
        }
        mvc.perform(post("/probe/INVALID")).andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value("COMMON-HTTP-405"));
    }
}
