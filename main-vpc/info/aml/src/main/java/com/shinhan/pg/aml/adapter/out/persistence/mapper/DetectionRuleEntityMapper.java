package com.shinhan.pg.aml.adapter.out.persistence.mapper;

import com.shinhan.pg.aml.adapter.out.persistence.entity.DetectionRuleEntity;
import com.shinhan.pg.aml.domain.model.DetectionRule;

public final class DetectionRuleEntityMapper {
    public DetectionRule toDomain(DetectionRuleEntity e) {
        return new DetectionRule(
                e.code(),
                e.version(),
                e.description(),
                e.score(),
                e.currency(),
                e.kinds(),
                e.minimumAmount(),
                e.minimumCount(),
                e.windowSeconds(),
                e.condition());
    }

    public DetectionRuleEntity toEntity(DetectionRule v) {
        return new DetectionRuleEntity(
                v.code(),
                v.version(),
                v.description(),
                v.score(),
                v.currency(),
                v.kinds(),
                v.minimumAmount(),
                v.minimumCount(),
                v.windowSeconds(),
                v.condition());
    }
}
