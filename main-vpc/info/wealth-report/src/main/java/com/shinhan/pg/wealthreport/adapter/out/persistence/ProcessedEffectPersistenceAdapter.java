package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.application.port.out.ProcessedEffectRepository;

import java.util.UUID;

/** TODO: 실제 소비 중복 방지 저장소. DB 구현과 원자성 검증 후 configuration에 조립한다. */
public final class ProcessedEffectPersistenceAdapter implements ProcessedEffectRepository {
    @Override
    public boolean claimOnce(String consumerName, UUID eventId, String effectKey) {
        throw new FeatureNotImplementedException("PROCESSED-EFFECT");
    }
}
