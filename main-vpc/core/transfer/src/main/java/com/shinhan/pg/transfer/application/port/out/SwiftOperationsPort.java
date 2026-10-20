package com.shinhan.pg.transfer.application.port.out;

import java.util.List;

public interface SwiftOperationsPort {
    List<String> history(String uetr);

    String query(String uetr);

    String retransmit(String uetr);
}
