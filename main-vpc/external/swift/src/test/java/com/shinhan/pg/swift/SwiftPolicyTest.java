package com.shinhan.pg.swift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.port.out.FepSwiftPort;
import com.shinhan.pg.swift.application.port.out.SwiftSubmissionStore;
import com.shinhan.pg.swift.application.port.out.TransferInstructionPort;
import com.shinhan.pg.swift.application.service.SwiftApplicationService;
import com.shinhan.pg.swift.config.SwiftDevelopmentGuard;
import com.shinhan.pg.swift.domain.policy.SwiftFinalityPolicy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.UUID;

class SwiftPolicyTest {
    @Test
    void receiptsAndIntermediateStatesAreNotFinal() {
        var p = new SwiftFinalityPolicy();
        for (String code : List.of("ACTC", "ACCP", "ACSP", "PDNG", "RCVD"))
            assertEquals("PROCESSING", p.classify(code));
        assertEquals("REVIEW", p.classify("ZZZZ"));
    }

    @Test
    void finalConflictIsStickyReview() {
        var p = new SwiftFinalityPolicy();
        assertEquals("ACSC", p.converge("ACSC", "PROCESSING"));
        assertEquals("REVIEW", p.converge("ACSC", "RJCT"));
        assertEquals("REVIEW", p.converge("REVIEW", "ACSC"));
    }

    @Test
    void retransmissionDisabledByDefaultEvenForValidKnownTransfer() {
        var fep = mock(FepSwiftPort.class);
        var store = mock(SwiftSubmissionStore.class);
        var service =
                new SwiftApplicationService(
                        fep, store, mock(TransferInstructionPort.class), true, false, 3, 30);
        assertThrows(
                SwiftProblem.class,
                () ->
                        service.retrySwift(
                                new RetrySwiftCommand(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID().toString())));
        verifyNoInteractions(fep, store);
    }

    @Test
    void resultQueryDoesNotCallSendOrRetry() {
        var fep = mock(FepSwiftPort.class);
        var store = mock(SwiftSubmissionStore.class);
        String uetr = UUID.randomUUID().toString();
        when(fep.queryResult(uetr))
                .thenReturn(
                        new com.shinhan.pg.swift.application.readmodel.SwiftQueryResult(
                                List.of(), false));
        var service =
                new SwiftApplicationService(
                        fep, store, mock(TransferInstructionPort.class), true, true, 3, 30);
        assertFalse(service.query(uetr));
        verify(fep).queryResult(uetr);
        verifyNoMoreInteractions(fep);
    }

    @Test
    void productionAndImplicitProfileActivationAreRejected() {
        var e =
                new MockEnvironment()
                        .withProperty(
                                "pg.swift.payload-key",
                                java.util.Base64.getEncoder().encodeToString(new byte[32]));
        assertThrows(IllegalStateException.class, () -> new SwiftDevelopmentGuard(e));
        e.setActiveProfiles("swift-test", "aws");
        assertThrows(IllegalStateException.class, () -> new SwiftDevelopmentGuard(e));
        e.setActiveProfiles("swift-test", "docker");
        new SwiftDevelopmentGuard(e);
    }

    @Test
    void unboundedOrZeroRetryConfigurationIsRejected() {
        assertThrows(
                SwiftProblem.class,
                () ->
                        new SwiftApplicationService(
                                mock(FepSwiftPort.class),
                                mock(SwiftSubmissionStore.class),
                                mock(TransferInstructionPort.class),
                                true,
                                true,
                                0,
                                30));
    }

    @Test
    void abandonedFepTransmissionCanBecomeUnknownWithoutRepeatingInitialSend() {
        var fep = mock(FepSwiftPort.class);
        var store = mock(SwiftSubmissionStore.class);
        String uetr = UUID.randomUUID().toString();
        when(fep.queryResult(uetr))
                .thenReturn(
                        new com.shinhan.pg.swift.application.readmodel.SwiftQueryResult(
                                List.of(), true));
        var service =
                new SwiftApplicationService(
                        fep, store, mock(TransferInstructionPort.class), true, true, 3, 30);
        assertFalse(service.query(uetr));
        verify(store).transmissionUnknown(uetr);
        verify(fep).queryResult(uetr);
        verifyNoMoreInteractions(fep);
    }
}
