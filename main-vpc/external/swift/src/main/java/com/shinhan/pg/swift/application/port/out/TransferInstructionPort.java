package com.shinhan.pg.swift.application.port.out;

import com.shinhan.pg.common.swiftmessages.Remittance;

import java.util.UUID;

public interface TransferInstructionPort {
    Remittance get(UUID id);
}
