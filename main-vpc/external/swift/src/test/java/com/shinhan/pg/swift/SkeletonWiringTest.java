package com.shinhan.pg.swift;

import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;
import com.shinhan.pg.swift.application.port.in.RecordSwiftAcknowledgementUseCase;
import com.shinhan.pg.swift.application.port.in.RetrySwiftUseCase;
import com.shinhan.pg.swift.application.port.in.SubmitSwiftUseCase;
import com.shinhan.pg.swift.config.SwiftSkeletonConfiguration;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                SwiftSkeletonConfiguration.class,
                SubmitSwiftUseCase.class,
                RetrySwiftUseCase.class,
                RecordSwiftAcknowledgementUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assembles(
                SwiftSkeletonConfiguration.class,
                SubmitSwiftUseCase.class,
                RetrySwiftUseCase.class,
                RecordSwiftAcknowledgementUseCase.class);
    }
}
