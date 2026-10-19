package com.shinhan.pg.transfer.domain.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;
import com.shinhan.pg.transfer.domain.model.Transfer;

import org.junit.jupiter.api.Test;

import java.util.UUID;

/** [WO-A6] 상태 전이표와 멱등키 정책의 불변식 검증. */
class TransferPolicyTest {
    private final TransferStatePolicy statePolicy = new TransferStatePolicy();
    private final TransferIdempotencyPolicy idempotencyPolicy = new TransferIdempotencyPolicy();

    private Transfer transferWith(String status) {
        return new Transfer(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of(1000),
                "key",
                "hash",
                UUID.randomUUID(),
                status);
    }

    @Test
    void allowedTransitions() {
        assertDoesNotThrow(() -> statePolicy.validateTransition("RECEIVED", "PROCESSING"));
        assertDoesNotThrow(() -> statePolicy.validateTransition("PROCESSING", "COMPLETED"));
        assertDoesNotThrow(() -> statePolicy.validateTransition("PROCESSING", "FAILED"));
        assertDoesNotThrow(() -> statePolicy.validateTransition("PROCESSING", "UNKNOWN"));
        assertDoesNotThrow(() -> statePolicy.validateTransition("UNKNOWN", "COMPLETED"));
        assertDoesNotThrow(() -> statePolicy.validateTransition("UNKNOWN", "FAILED"));
    }

    @Test
    void terminalStatesNeverTransit() {
        for (var terminal : new String[] {"COMPLETED", "FAILED"}) {
            assertTrue(statePolicy.isTerminal(terminal));
            for (var next :
                    new String[] {"RECEIVED", "PROCESSING", "COMPLETED", "FAILED", "UNKNOWN"}) {
                var error =
                        assertThrows(
                                TransferException.class,
                                () -> statePolicy.validateTransition(transferWith(terminal), next));
                assertEquals(TransferErrorCode.INVALID_STATE, error.code());
            }
        }
        // UNKNOWN에서 RECEIVED/PROCESSING으로 되돌리지 않는다.
        assertThrows(
                TransferException.class,
                () -> statePolicy.validateTransition("UNKNOWN", "PROCESSING"));
        assertThrows(
                TransferException.class,
                () -> statePolicy.validateTransition("RECEIVED", "COMPLETED"));
    }

    @Test
    void duplicateRequestAlways409WithExistingId() {
        var original = transferWith("PROCESSING");
        var sameHash =
                assertThrows(
                        TransferException.class,
                        () -> idempotencyPolicy.validateDuplicateRequest(original, "hash"));
        assertEquals(TransferErrorCode.DUPLICATE_REQUEST, sameHash.code());
        assertEquals(original.id(), sameHash.existingTransferId());

        var otherHash =
                assertThrows(
                        TransferException.class,
                        () -> idempotencyPolicy.validateDuplicateRequest(original, "other"));
        assertEquals(TransferErrorCode.DUPLICATE_REQUEST, otherHash.code());
        assertEquals(original.id(), otherHash.existingTransferId());
    }
}
