package com.shinhan.pg.aigateway.adapter.out.persistence.mapper;

import com.shinhan.pg.aigateway.adapter.out.persistence.entity.AiRequestAuditEntity;
import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;
import com.shinhan.pg.aigateway.domain.model.PiiType;
import com.shinhan.pg.aigateway.domain.model.PreparationDecision;
import com.shinhan.pg.aigateway.domain.model.PreparationReason;
import java.util.EnumMap;

public final class AiRequestAuditEntityMapper {
    public AiRequestAudit toDomain(AiRequestAuditEntity row) {
        var counts = new EnumMap<PiiType, Integer>(PiiType.class);
        counts.put(PiiType.NAME, row.nameCount()); counts.put(PiiType.RRN, row.rrnCount());
        counts.put(PiiType.ACCOUNT, row.accountCount()); counts.put(PiiType.PHONE, row.phoneCount());
        counts.put(PiiType.EMAIL, row.emailCount()); counts.put(PiiType.CARD, row.cardCount());
        return new AiRequestAudit(row.requestId(), row.callerServiceId(),
                PreparationDecision.valueOf(row.decision()), PreparationReason.valueOf(row.reason()),
                counts, row.policyVersion(), row.createdAt());
    }
    public AiRequestAuditEntity toEntity(AiRequestAudit value) {
        var c = value.maskedCounts();
        return new AiRequestAuditEntity(value.requestId(), value.callerServiceId(),
                value.decision().name(), value.reason().name(), c.getOrDefault(PiiType.NAME, 0),
                c.getOrDefault(PiiType.RRN, 0), c.getOrDefault(PiiType.ACCOUNT, 0),
                c.getOrDefault(PiiType.PHONE, 0), c.getOrDefault(PiiType.EMAIL, 0), c.getOrDefault(PiiType.CARD, 0),
                value.policyVersion(), value.createdAt());
    }
}
