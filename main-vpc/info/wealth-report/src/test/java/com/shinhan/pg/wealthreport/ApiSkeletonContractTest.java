package com.shinhan.pg.wealthreport;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.wealthreport.adapter.in.web.ReportOperationsController;

import org.junit.jupiter.api.Test;

class ApiSkeletonContractTest {
    @Test
    void heldApisDoNotPublishMappings() {
        ApiSkeletonAssertions.heldApisHaveNoMappings(ReportOperationsController.class);
    }
}
