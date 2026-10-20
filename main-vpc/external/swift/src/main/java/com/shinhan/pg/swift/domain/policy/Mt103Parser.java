package com.shinhan.pg.swift.domain.policy;

import com.shinhan.pg.swift.domain.model.ParsedSwiftTransfer;

/**
 *
 *
 * <pre>
 * [MVP5-F03] MT103 입력이 있으면 pacs.008 필드로 변환할 위치. MT103/MX 전체 지원을 주장하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class Mt103Parser {
    public ParsedSwiftTransfer parse(String rawMt103) {
        throw com.shinhan.pg.common.swiftmessages.SwiftProblem
                .invalid(); // UETR must come from the stored transfer, never invented by a parser.
    }

    public com.shinhan.pg.common.swiftmessages.Remittance parse(
            java.util.UUID transferId, String uetr, String rawMt103) {
        return com.shinhan.pg.common.swiftmessages.Mt103Subset.parse(transferId, uetr, rawMt103);
    }
}
