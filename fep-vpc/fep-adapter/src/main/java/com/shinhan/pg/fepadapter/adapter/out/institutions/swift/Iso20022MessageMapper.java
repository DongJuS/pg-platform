package com.shinhan.pg.fepadapter.adapter.out.institutions.swift;

/**
 *
 *
 * <pre>
 * [MVP5-F04] TODO: pacs.008 생성·XSD 검증과 pacs.002 파싱을 기관 경계에서 처리한다.
 * XML 외부 엔티티를 차단하고 지원 버전/필드·거절 코드를 계약으로 확정한다.
 * 합법적인 실제 SWIFT망 송금 구현을 뜻하지 않으며 Mock 전문만 대상으로 한다.
 * </pre>
 */
public final class Iso20022MessageMapper {
    public String toPacs008(String validatedTransferData) {
        throw com.shinhan.pg.common.swiftmessages.SwiftProblem
                .invalid(); // Untyped strings are not validated transfer data.
    }

    public String readPacs002Status(String responseXml) {
        return com.shinhan.pg.common.swiftmessages.Iso20022.result(responseXml).code();
    }

    public String toPacs008(
            com.shinhan.pg.common.swiftmessages.Remittance data,
            java.time.Instant frozenCreatedAt) {
        return com.shinhan.pg.common.swiftmessages.Iso20022.payment(data, frozenCreatedAt);
    }
}
