package com.shinhan.pg.account.domain.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.account.domain.error.AccountErrorCode;
import com.shinhan.pg.account.domain.model.Account;
import com.shinhan.pg.common.commonerror.ApplicationException;

import org.junit.jupiter.api.Test;

import java.util.UUID;

/** [MVP1-F10] 허용 전이는 ACTIVE↔SUSPENDED 뿐이다. */
class AccountStatusPolicyTest {
    private final AccountStatusPolicy policy = new AccountStatusPolicy();

    private static Account accountWith(String status) {
        return new Account(UUID.randomUUID(), UUID.randomUUID(), "1234567890123456", "KRW", "별칭", status);
    }

    @Test
    void activeToSuspendedIsAllowed() {
        assertDoesNotThrow(
                () -> policy.validateTransition(accountWith("ACTIVE"), "SUSPENDED"));
    }

    @Test
    void suspendedToActiveIsAllowed() {
        assertDoesNotThrow(
                () -> policy.validateTransition(accountWith("SUSPENDED"), "ACTIVE"));
    }

    @Test
    void sameStatusTransitionIsRejected() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () -> policy.validateTransition(accountWith("ACTIVE"), "ACTIVE"));
        assertEquals(AccountErrorCode.INVALID_STATUS_TRANSITION, error.errorCode());
    }

    @Test
    void unknownStatusIsRejected() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () -> policy.validateTransition(accountWith("ACTIVE"), "CLOSED"));
        assertEquals(AccountErrorCode.INVALID_STATUS_TRANSITION, error.errorCode());
    }

    @Test
    void nullStatusIsRejected() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () -> policy.validateTransition(accountWith("ACTIVE"), null));
        assertEquals(AccountErrorCode.INVALID_STATUS_TRANSITION, error.errorCode());
    }
}
