package com.shinhan.pg.aigateway;

import com.shinhan.pg.aigateway.adapter.in.web.AiAuditController;
import com.shinhan.pg.aigateway.adapter.in.web.AiInferenceController;
import com.shinhan.pg.aigateway.adapter.in.web.AiStatisticsController;
import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;

import org.junit.jupiter.api.Test;

class ApiSkeletonContractTest {
    @Test
    void heldApisDoNotPublishMappings() {
        ApiSkeletonAssertions.heldApisHaveNoMappings(
                AiInferenceController.class, AiAuditController.class, AiStatisticsController.class);
    }
}
