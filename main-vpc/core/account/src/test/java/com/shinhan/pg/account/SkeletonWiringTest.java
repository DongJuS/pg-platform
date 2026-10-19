package com.shinhan.pg.account;

import com.shinhan.pg.account.application.port.in.ChangeAccountStatusUseCase;
import com.shinhan.pg.account.application.port.in.GetAccountUseCase;
import com.shinhan.pg.account.application.port.in.ListAccountsUseCase;
import com.shinhan.pg.account.application.port.in.OpenAccountUseCase;
import com.shinhan.pg.account.config.AccountSkeletonConfiguration;
import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                AccountSkeletonConfiguration.class,
                OpenAccountUseCase.class,
                ListAccountsUseCase.class,
                GetAccountUseCase.class,
                ChangeAccountStatusUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assemblesWithInfrastructure(
                AccountSkeletonConfiguration.class,
                OpenAccountUseCase.class,
                ListAccountsUseCase.class,
                GetAccountUseCase.class,
                ChangeAccountStatusUseCase.class);
    }
}
