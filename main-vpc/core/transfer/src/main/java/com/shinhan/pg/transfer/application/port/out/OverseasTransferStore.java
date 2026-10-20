package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.common.eventcontracts.OverseasLedgerAppliedEvent;
import com.shinhan.pg.transfer.application.command.RequestOverseasTransferCommand;
import com.shinhan.pg.transfer.application.readmodel.OverseasSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OverseasTransferStore extends OverseasTransferDetailsRepository {
    Optional<OverseasSnapshot> find(UUID id);

    Optional<OverseasSnapshot> find(UUID actor, String key);

    OverseasSnapshot create(RequestOverseasTransferCommand command, String hash);

    boolean claim(UUID id, UUID worker);

    void ready(UUID id, UUID worker);

    void uncertain(UUID id, UUID worker, boolean rejected);

    List<UUID> recoverable();

    void applied(OverseasLedgerAppliedEvent event);

    List<String> audit(UUID id, boolean administrator);

    void purgePrivateData();
}
