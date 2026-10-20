package com.shinhan.pg.transfer.application.service;

import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferStore;
import com.shinhan.pg.transfer.application.port.out.SwiftOperationsPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Administrator IDs come from trusted deployment configuration; user-supplied role headers grant
 * nothing.
 */
public final class OverseasOperationsService {
    private final Set<UUID> administrators;
    private final OverseasTransferStore store;
    private final SwiftOperationsPort swift;

    public OverseasOperationsService(
            Set<UUID> administrators, OverseasTransferStore store, SwiftOperationsPort swift) {
        this.administrators = Set.copyOf(administrators);
        this.store = store;
        this.swift = swift;
    }

    public List<String> history(UUID actor, UUID id) {
        requireActor(actor);
        var row =
                store.find(id)
                        .orElseThrow(() -> new SwiftProblem("NOT_FOUND", ErrorType.NOT_FOUND));
        boolean admin = administrators.contains(actor);
        if (!admin && !actor.equals(row.actorUserId()))
            throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
        var results = new ArrayList<>(store.audit(id, admin));
        if (admin
                && row.remittance() != null
                && !java.util.Set.of("RESERVING", "UNKNOWN", "FAILED").contains(row.status()))
            results.addAll(swift.history(row.uetr()));
        return List.copyOf(results);
    }

    public String action(UUID actor, UUID id, boolean retransmit) {
        requireActor(actor);
        if (!administrators.contains(actor))
            throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
        var row =
                store.find(id)
                        .orElseThrow(() -> new SwiftProblem("NOT_FOUND", ErrorType.NOT_FOUND));
        return retransmit ? swift.retransmit(row.uetr()) : swift.query(row.uetr());
    }

    private static void requireActor(UUID actor) {
        if (actor == null) throw new SwiftProblem("UNAUTHORIZED", ErrorType.UNAUTHORIZED);
    }
}
