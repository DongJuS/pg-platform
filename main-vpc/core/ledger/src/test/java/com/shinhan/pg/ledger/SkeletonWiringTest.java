package com.shinhan.pg.ledger;

import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;
import com.shinhan.pg.ledger.application.port.in.ApplyInstitutionResultUseCase;
import com.shinhan.pg.ledger.application.port.in.GetBalanceUseCase;
import com.shinhan.pg.ledger.application.port.in.ListJournalEntriesUseCase;
import com.shinhan.pg.ledger.application.port.in.PostJournalUseCase;
import com.shinhan.pg.ledger.application.port.in.QueryJournalUseCase;
import com.shinhan.pg.ledger.config.LedgerSkeletonConfiguration;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                LedgerSkeletonConfiguration.class,
                PostJournalUseCase.class,
                ApplyInstitutionResultUseCase.class,
                QueryJournalUseCase.class,
                GetBalanceUseCase.class,
                ListJournalEntriesUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assemblesWithInfrastructure(
                LedgerSkeletonConfiguration.class,
                PostJournalUseCase.class,
                ApplyInstitutionResultUseCase.class,
                QueryJournalUseCase.class,
                GetBalanceUseCase.class,
                ListJournalEntriesUseCase.class);
    }
}
