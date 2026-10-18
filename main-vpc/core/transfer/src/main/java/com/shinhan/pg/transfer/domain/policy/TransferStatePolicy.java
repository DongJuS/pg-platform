package com.shinhan.pg.transfer.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.transfer.domain.model.Transfer;

/**
 * <pre>
 * [MVP1-F16,MVP1-F20] RECEIVED/PROCESSING/COMPLETED/FAILED/UNKNOWN의 허용 전이를 정의한다. UNKNOWN을 404만으로 실패 처리하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class TransferStatePolicy {
    public void validateTransition(Transfer current, String nextStatus) {
        throw new FeatureNotImplementedException("MVP1-F16");
    }
}
