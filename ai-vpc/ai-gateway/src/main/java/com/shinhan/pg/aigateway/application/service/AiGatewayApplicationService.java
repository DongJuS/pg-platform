package com.shinhan.pg.aigateway.application.service;

import com.shinhan.pg.aigateway.application.command.PrepareAiRequestCommand;
import com.shinhan.pg.aigateway.application.port.in.PrepareAiRequestUseCase;
import com.shinhan.pg.aigateway.application.port.out.AiAuditWritePort;
import com.shinhan.pg.aigateway.application.port.out.PiiInspectionPort;
import com.shinhan.pg.aigateway.application.result.PrepareAiRequestResult;
import com.shinhan.pg.aigateway.domain.error.AiPreparationException;
import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;
import com.shinhan.pg.aigateway.domain.model.PiiType;
import com.shinhan.pg.aigateway.domain.model.PreparationDecision;
import com.shinhan.pg.aigateway.domain.model.PreparationReason;
import com.shinhan.pg.aigateway.domain.policy.AiRequestInspectionPolicy;
import com.shinhan.pg.aigateway.domain.policy.PiiMaskingPolicy;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MVP3 pre-transfer boundary: authorize -> validate -> mask -> residual inspection -> input policy
 * -> commit minimal audit -> return. No LlmRelayPort, provider, model, API key or network client.
 * Figma 1:15 / 19:621; user scope stops before external transfer and forbids prompt persistence.
 * Policy/model quotas, inference, response filtering and restoration remain deferred.
 */
public final class AiGatewayApplicationService implements PrepareAiRequestUseCase {
    private final PiiMaskingPolicy masking;
    private final PiiInspectionPort inspection;
    private final AiRequestInspectionPolicy policy;
    private final AiAuditWritePort audits;
    private final Set<String> allowedCallers;
    private final Clock clock;

    public AiGatewayApplicationService(PiiMaskingPolicy masking, PiiInspectionPort inspection,
            AiRequestInspectionPolicy policy, AiAuditWritePort audits, Set<String> allowedCallers, Clock clock) {
        this.masking = java.util.Objects.requireNonNull(masking);
        this.inspection = java.util.Objects.requireNonNull(inspection);
        this.policy = java.util.Objects.requireNonNull(policy);
        this.audits = java.util.Objects.requireNonNull(audits);
        this.allowedCallers = Set.copyOf(allowedCallers);
        this.clock = java.util.Objects.requireNonNull(clock);
    }

    @Override public PrepareAiRequestResult prepare(PrepareAiRequestCommand command) {
        // Authenticator binds this value to a caller-specific token before the Controller executes.
        if (command == null || command.callerServiceId() == null
                || !command.callerServiceId().matches("[a-z][a-z0-9-]{0,47}"))
            throw new AiPreparationException(PreparationReason.FORBIDDEN_CALLER);
        UUID id = UUID.randomUUID();
        var decision = PreparationDecision.BLOCKED;
        var reason = PreparationReason.FORBIDDEN_CALLER;
        Map<PiiType, Integer> counts = Map.of();
        String masked = null;
        if (allowedCallers.contains(command.callerServiceId())) {
            try {
                if (command.inputFormat() == null || command.inputFormat().isBlank())
                    throw new AiPreparationException(PreparationReason.INVALID_INPUT);
                if (!"TEXT".equals(command.inputFormat()))
                    throw new AiPreparationException(PreparationReason.UNSUPPORTED_FORMAT);
                masking.validate(command.prompt(), command.sensitiveValues());
                var result = masking.mask(command.prompt(), command.sensitiveValues());
                counts = result.counts();
                if (!inspection.isClear(result.text(), command.sensitiveValues())) {
                    reason = PreparationReason.RESIDUAL_PII;
                } else if (policy.blocks(command.prompt()) || policy.blocks(result.text())) {
                    // Inspect the original too: masking must not erase a deny-rule trigger.
                    reason = PreparationReason.PROMPT_INJECTION;
                } else {
                    decision = PreparationDecision.READY_FOR_TRANSFER;
                    reason = PreparationReason.CHECKS_PASSED;
                    masked = result.text();
                }
            } catch (AiPreparationException invalid) {
                reason = invalid.reason();
            } catch (RuntimeException inspectionFailure) {
                // A dependency can include PII in its error text. Do not chain or log that exception.
                decision = PreparationDecision.ERROR;
                reason = PreparationReason.INSPECTION_UNAVAILABLE;
            }
        }
        try {
            audits.record(new AiRequestAudit(id, command.callerServiceId(), decision, reason,
                    counts, AiRequestInspectionPolicy.VERSION, Instant.now(clock)));
        } catch (RuntimeException auditFailure) {
            throw new AiPreparationException(PreparationReason.AUDIT_UNAVAILABLE);
        }
        return new PrepareAiRequestResult(id, decision, reason, masked, counts, AiRequestInspectionPolicy.VERSION);
    }
}
