package com.shinhan.pg.common.swiftmessages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

class SwiftMessagesTest {
    private Remittance r() {
        return new Remittance(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "REF123",
                LocalDate.of(2026, 10, 20),
                "SYNTHETIC SENDER",
                "SOURCE123",
                "Receiver & Co",
                "BENEFICIARY123",
                "TESTUS33XXX",
                new BigDecimal("12.34"),
                "USD",
                "Invoice <A>",
                "SHA");
    }

    @Test
    void mtAndCompleteIsoSchemasRoundTrip() {
        var r = r();
        var parsed = Mt103Subset.parse(r.transferId(), r.uetr(), Mt103Subset.write(r));
        assertEquals(r, parsed);
        String xml = Iso20022.payment(r, Instant.EPOCH);
        assertEquals(r, Iso20022.parsePayment(r.transferId(), xml));
    }

    @Test
    void statusCarriesAllCorrelationKeys() {
        var r = r();
        var result = new Iso20022.Result("REPORT1", r.reference(), r.uetr(), "ACSC");
        assertEquals(result, Iso20022.result(Iso20022.status(result, Instant.EPOCH)));
    }

    @Test
    void completeSchemaRejectsMissingRequiredElement() {
        var r = r();
        var xml = Iso20022.payment(r, Instant.EPOCH).replace("<ChrgBr>SHAR</ChrgBr>", "");
        assertThrows(SwiftProblem.class, () -> Iso20022.parsePayment(r.transferId(), xml));
    }

    @Test
    void rejectsForeignNamespaceAndVersion() {
        var r = r();
        var xml = Iso20022.payment(r, Instant.EPOCH).replace("pacs.008.001.08", "pacs.008.001.09");
        assertThrows(SwiftProblem.class, () -> Iso20022.parsePayment(r.transferId(), xml));
    }

    @Test
    void dtdAndExternalEntitiesAreRejectedWithoutLeakingDiagnostics() {
        String xml =
                "<!DOCTYPE Document [<!ENTITY private SYSTEM"
                        + " 'file:///etc/passwd'>]><Document>&private;</Document>";
        var e =
                assertThrows(
                        SwiftProblem.class, () -> Iso20022.parsePayment(UUID.randomUUID(), xml));
        assertFalse(e.getMessage().contains("passwd"));
    }

    @Test
    void rejectsMultiTransactionAndUnsupportedFx() {
        var r = r();
        var xml = Iso20022.payment(r, Instant.EPOCH);
        assertThrows(
                SwiftProblem.class,
                () ->
                        Iso20022.parsePayment(
                                r.transferId(), xml.replace("<NbOfTxs>1", "<NbOfTxs>2")));
        assertThrows(
                SwiftProblem.class,
                () ->
                        Iso20022.parsePayment(
                                r.transferId(),
                                xml.replace("<ChrgBr>", "<XchgRate>1.0</XchgRate><ChrgBr>")));
    }

    @Test
    void rejectsDuplicateUnknownAndOutOfOrderMtFields() {
        var r = r();
        String mt = Mt103Subset.write(r);
        for (String bad :
                java.util.List.of(
                        mt.replace(":23B:", ":20:SECOND\n:23B:"),
                        mt.replace(":57A:", ":99X:"),
                        mt.replace(":20:", ":59:"))) {
            assertThrows(
                    SwiftProblem.class, () -> Mt103Subset.parse(r.transferId(), r.uetr(), bad));
        }
    }

    @Test
    void rejectsLossyAmountsCurrenciesFeesAndDates() {
        var r = r();
        String mt = Mt103Subset.write(r);
        for (String bad :
                java.util.List.of(
                        mt.replace("12,34", "12,345"),
                        mt.replace("USD", "EUR"),
                        mt.replace(":71A:SHA", ":71A:OUR"),
                        mt.replace("261020", "260230"))) {
            assertThrows(
                    SwiftProblem.class, () -> Mt103Subset.parse(r.transferId(), r.uetr(), bad));
        }
    }

    @Test
    void rejectsResultWithoutOriginalUetrAndCorrelation() {
        var r = r();
        String xml =
                Iso20022.status(
                        new Iso20022.Result("REPORT1", r.reference(), r.uetr(), "ACSC"),
                        Instant.EPOCH);
        assertThrows(
                SwiftProblem.class,
                () -> Iso20022.result(xml.replace("<OrgnlUETR>" + r.uetr() + "</OrgnlUETR>", "")));
        assertThrows(
                SwiftProblem.class,
                () -> Iso20022.result(xml.replace("<OrgnlTxId>REF123", "<OrgnlTxId>OTHER")));
    }

    @Test
    void encryptionUsesNoncesAndBindsRecordIdentity() {
        var c = new PrivatePayload(java.util.Base64.getEncoder().encodeToString(new byte[32]));
        String a = c.encrypt("a", "PRIVATE");
        String b = c.encrypt("a", "PRIVATE");
        assertNotEquals(a, b);
        assertEquals("PRIVATE", c.decrypt("a", a));
        assertThrows(SwiftProblem.class, () -> c.decrypt("b", a));
        assertFalse(a.contains("PRIVATE"));
    }

    @Test
    void stableBusinessIdentitiesAreDifferentForHoldAndFinal() {
        var id = UUID.randomUUID();
        assertEquals(SwiftIds.id(id, "hold"), SwiftIds.id(id, "hold"));
        assertNotEquals(SwiftIds.id(id, "hold"), SwiftIds.id(id, "final"));
    }

    @Test
    void uetrRequiresCanonicalVersionFour() {
        assertThrows(
                SwiftProblem.class,
                () -> SwiftIds.uetr(SwiftIds.id(UUID.randomUUID(), "hold").toString()));
        assertThrows(SwiftProblem.class, () -> SwiftIds.uetr("bad"));
    }

    @Test
    void payloadSizeIsBoundedBeforeXmlParsing() {
        assertThrows(
                SwiftProblem.class,
                () -> Iso20022.parsePayment(UUID.randomUUID(), "x".repeat(65537)));
    }
}
