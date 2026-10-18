package com.shinhan.pg.swift;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.swift.adapter.in.web.SwiftOperationsController;

import org.junit.jupiter.api.Test;

class ApiSkeletonContractTest {
    @Test
    void heldApisDoNotPublishMappings() {
        ApiSkeletonAssertions.heldApisHaveNoMappings(SwiftOperationsController.class);
    }
}
