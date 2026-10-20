package com.shinhan.pg.aigateway.adapter.in.web;

import com.shinhan.pg.aigateway.adapter.in.web.request.PrepareAiRequest;
import com.shinhan.pg.aigateway.adapter.in.web.response.PrepareAiResponse;
import com.shinhan.pg.aigateway.application.command.PrepareAiRequestCommand;
import com.shinhan.pg.aigateway.application.port.in.PrepareAiRequestUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /internal/v1/ai/preparations. Authenticated service only; callerServiceId is not a body field.
 * TEXT+prompt+sensitiveValues -> requestId, maskedPrompt (ready only), status, safe reason, counts.
 * 200 pre-transfer checks complete; 400 invalid input; 403 authorization/policy; 413 size;
 * 415 format; 503 inspection/audit failure. No LLM invocation, no generated answer, no async job.
 * Figma 1:15, 19:621; Notion inference draft remains deferred, user scope defines this boundary.
 */
@RestController
@Profile("skeleton")
public final class AiPreparationController {
    private final PrepareAiRequestUseCase preparations;
    private final MeterRegistry meters;
    public AiPreparationController(PrepareAiRequestUseCase preparations, MeterRegistry meters) {
        this.preparations = preparations; this.meters = meters;
    }
    @PostMapping(value = "/internal/v1/ai/preparations", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PrepareAiResponse> prepare(@RequestAttribute("callerServiceId") String caller,
            @RequestBody PrepareAiRequest request) {
        var result = preparations.prepare(new PrepareAiRequestCommand(caller, request.inputFormat(),
                request.prompt(), request.sensitiveValues()));
        // Closed enum labels only: never caller IDs, prompts, request IDs, counts or credential values.
        meters.counter("pg.ai.preparation.decisions", "decision", result.status().name(),
                "reason", result.reason().name()).increment();
        LoggerFactory.getLogger(getClass()).info("eventType=ai_preparation decision={} reason={}",
                result.status(), result.reason());
        return ResponseEntity.status(AiPreparationExceptionHandler.status(result.reason()))
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(PrepareAiResponse.from(result));
    }
}
