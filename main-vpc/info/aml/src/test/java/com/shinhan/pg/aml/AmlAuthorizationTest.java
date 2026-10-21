package com.shinhan.pg.aml;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shinhan.pg.aml.application.command.ListAmlCasesCommand;
import com.shinhan.pg.aml.application.port.out.AmlAtomicWritePort;
import com.shinhan.pg.aml.application.port.out.AmlSourcePort;
import com.shinhan.pg.aml.application.port.out.FepKofiuPort;
import com.shinhan.pg.aml.application.service.AmlApplicationService;
import com.shinhan.pg.common.amlcontracts.AmlProblem;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class AmlAuthorizationTest {
    @Test
    void ordinaryAndForgedCallersCannotReadAnyCase() {
        var store = mock(AmlAtomicWritePort.class);
        var service =
                new AmlApplicationService(
                        mock(FepKofiuPort.class),
                        store,
                        mock(AmlSourcePort.class),
                        null,
                        true,
                        false,
                        false,
                        false);
        for (String caller : new String[] {"gateway", "transfer", "aml", "user", "admin", ""}) {
            assertThrows(AmlProblem.class, () -> service.getCase(caller, UUID.randomUUID()));
            assertThrows(
                    AmlProblem.class,
                    () ->
                            service.listAmlCases(
                                    new ListAmlCasesCommand(
                                            UUID.randomUUID(), caller, null, null, null, 0, 20)));
        }
        verifyNoInteractions(store);
    }

    @Test
    void invalidPaginationIsRejectedBeforeStorage() {
        var store = mock(AmlAtomicWritePort.class);
        var service =
                new AmlApplicationService(
                        mock(FepKofiuPort.class), store, null, null, true, false, false, false);
        assertThrows(
                AmlProblem.class,
                () ->
                        service.listAmlCases(
                                new ListAmlCasesCommand(
                                        UUID.randomUUID(),
                                        "aml-operations",
                                        null,
                                        null,
                                        null,
                                        -1,
                                        20)));
        assertThrows(
                AmlProblem.class,
                () ->
                        service.listAmlCases(
                                new ListAmlCasesCommand(
                                        UUID.randomUUID(),
                                        "aml-operations",
                                        null,
                                        null,
                                        null,
                                        0,
                                        101)));
        verifyNoInteractions(store);
    }

    @Test
    void dispatchAndRetransmissionStayDisabledWithoutExplicitConfiguration() {
        var store = mock(AmlAtomicWritePort.class);
        var fep = mock(FepKofiuPort.class);
        var service = new AmlApplicationService(fep, store, null, null, true, false, false, false);
        assertThrows(
                AmlProblem.class, () -> service.retransmit("aml-operations", UUID.randomUUID()));
        verifyNoInteractions(fep);
    }
}
