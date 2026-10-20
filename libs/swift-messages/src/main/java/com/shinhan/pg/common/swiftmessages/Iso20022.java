package com.shinhan.pg.common.swiftmessages;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * A single-transaction development subset, validated against complete published ISO XSDs. No CBPR+,
 * FIN transport, BAH, sanctions, correspondent routing or bank certification claim.
 */
public final class Iso20022 {
    public static final String PAYMENT = "pacs.008.001.08";
    public static final String STATUS = "pacs.002.001.10";
    private static final String NS = "urn:iso:std:iso:20022:tech:xsd:";
    private static final Schema PAYMENT_SCHEMA = schema(PAYMENT);
    private static final Schema STATUS_SCHEMA = schema(STATUS);

    private Iso20022() {}

    public static String payment(Remittance r, Instant createdAt) {
        String xml =
                "<Document xmlns=\""
                        + NS
                        + PAYMENT
                        + "\"><FIToFICstmrCdtTrf><GrpHdr>"
                        + tag("MsgId", r.reference())
                        + tag("CreDtTm", createdAt.toString())
                        + "<NbOfTxs>1</NbOfTxs>"
                        + "<SttlmInf><SttlmMtd>INDA</SttlmMtd></SttlmInf></GrpHdr><CdtTrfTxInf><PmtId>"
                        + tag("EndToEndId", r.reference())
                        + tag("TxId", r.reference())
                        + tag("UETR", r.uetr())
                        + "</PmtId>"
                        + "<IntrBkSttlmAmt Ccy=\"USD\">"
                        + r.amount().toPlainString()
                        + "</IntrBkSttlmAmt>"
                        + tag("IntrBkSttlmDt", r.valueDate().toString())
                        + "<ChrgBr>SHAR</ChrgBr>"
                        + "<Dbtr>"
                        + tag("Nm", r.senderName())
                        + "</Dbtr>"
                        + account("DbtrAcct", r.senderAccount())
                        + "<DbtrAgt><FinInstnId><BICFI>SHBKKRSEXXX</BICFI></FinInstnId></DbtrAgt>"
                        + "<CdtrAgt><FinInstnId>"
                        + tag("BICFI", r.bic())
                        + "</FinInstnId></CdtrAgt>"
                        + "<Cdtr>"
                        + tag("Nm", r.recipientName())
                        + "</Cdtr>"
                        + account("CdtrAcct", r.recipientAccount())
                        + (r.remittanceInformation() == null
                                ? ""
                                : "<RmtInf>"
                                        + tag("Ustrd", r.remittanceInformation())
                                        + "</RmtInf>")
                        + "</CdtTrfTxInf></FIToFICstmrCdtTrf></Document>";
        parsePayment(r.transferId(), xml);
        return xml;
    }

    public static Remittance parsePayment(UUID transferId, String xml) {
        Element body = child(document(xml, PAYMENT), "FIToFICstmrCdtTrf");
        Element header = child(body, "GrpHdr");
        SwiftProblem.require("1".equals(value(header, "NbOfTxs")));
        Element transaction = child(body, "CdtTrfTxInf");
        Element identity = child(transaction, "PmtId");
        String reference = value(header, "MsgId");
        SwiftProblem.require(
                reference.equals(value(identity, "EndToEndId"))
                        && reference.equals(value(identity, "TxId")));
        SwiftProblem.require("SHAR".equals(value(transaction, "ChrgBr")));
        SwiftProblem.require("INDA".equals(value(child(header, "SttlmInf"), "SttlmMtd")));
        SwiftProblem.require(
                "SHBKKRSEXXX"
                        .equals(
                                value(
                                        child(child(transaction, "DbtrAgt"), "FinInstnId"),
                                        "BICFI")));
        // Accept precisely this subset. Do not silently discard fees, FX, extra parties or routing.
        allowed(body, "GrpHdr", "CdtTrfTxInf");
        allowed(header, "MsgId", "CreDtTm", "NbOfTxs", "SttlmInf");
        allowed(
                transaction,
                "PmtId",
                "IntrBkSttlmAmt",
                "IntrBkSttlmDt",
                "ChrgBr",
                "Dbtr",
                "DbtrAcct",
                "DbtrAgt",
                "CdtrAgt",
                "Cdtr",
                "CdtrAcct",
                "RmtInf");
        allowed(identity, "EndToEndId", "TxId", "UETR");
        allowed(child(header, "SttlmInf"), "SttlmMtd");
        for (String party : java.util.List.of("Dbtr", "Cdtr"))
            allowed(child(transaction, party), "Nm");
        for (String agent : java.util.List.of("DbtrAgt", "CdtrAgt")) {
            allowed(child(transaction, agent), "FinInstnId");
            allowed(child(child(transaction, agent), "FinInstnId"), "BICFI");
        }
        Element amount = child(transaction, "IntrBkSttlmAmt");
        Element information = optional(transaction, "RmtInf");
        if (information != null) allowed(information, "Ustrd");
        try {
            return new Remittance(
                    transferId,
                    value(identity, "UETR"),
                    reference,
                    LocalDate.parse(value(transaction, "IntrBkSttlmDt")),
                    value(child(transaction, "Dbtr"), "Nm"),
                    accountValue(child(transaction, "DbtrAcct")),
                    value(child(transaction, "Cdtr"), "Nm"),
                    accountValue(child(transaction, "CdtrAcct")),
                    value(child(child(transaction, "CdtrAgt"), "FinInstnId"), "BICFI"),
                    new BigDecimal(amount.getTextContent()),
                    amount.getAttribute("Ccy"),
                    information == null ? null : value(information, "Ustrd"),
                    "SHA");
        } catch (RuntimeException invalid) {
            throw SwiftProblem.invalid();
        }
    }

    public record Result(String eventReference, String messageReference, String uetr, String code) {
        public Result {
            Remittance.text(eventReference, 35);
            Remittance.text(messageReference, 16);
            SwiftIds.uetr(uetr);
            SwiftProblem.require(code != null && code.matches("[A-Z]{4}"));
        }
    }

    public static String status(Result r, Instant createdAt) {
        String xml =
                "<Document xmlns=\""
                        + NS
                        + STATUS
                        + "\"><FIToFIPmtStsRpt><GrpHdr>"
                        + tag("MsgId", r.eventReference())
                        + tag("CreDtTm", createdAt.toString())
                        + "</GrpHdr><TxInfAndSts><OrgnlGrpInf>"
                        + tag("OrgnlMsgId", r.messageReference())
                        + tag("OrgnlMsgNmId", PAYMENT)
                        + "</OrgnlGrpInf>"
                        + tag("OrgnlEndToEndId", r.messageReference())
                        + tag("OrgnlTxId", r.messageReference())
                        + tag("OrgnlUETR", r.uetr())
                        + tag("TxSts", r.code())
                        + "</TxInfAndSts></FIToFIPmtStsRpt></Document>";
        result(xml);
        return xml;
    }

    public static Result result(String xml) {
        Element body = child(document(xml, STATUS), "FIToFIPmtStsRpt");
        allowed(body, "GrpHdr", "TxInfAndSts");
        Element tx = child(body, "TxInfAndSts");
        allowed(tx, "OrgnlGrpInf", "OrgnlEndToEndId", "OrgnlTxId", "OrgnlUETR", "TxSts");
        Element original = child(tx, "OrgnlGrpInf");
        String ref = value(original, "OrgnlMsgId");
        SwiftProblem.require(
                PAYMENT.equals(value(original, "OrgnlMsgNmId"))
                        && ref.equals(value(tx, "OrgnlEndToEndId"))
                        && ref.equals(value(tx, "OrgnlTxId")));
        return new Result(
                value(child(body, "GrpHdr"), "MsgId"),
                ref,
                value(tx, "OrgnlUETR"),
                value(tx, "TxSts"));
    }

    private static Schema schema(String type) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(Iso20022.class.getResource("/xsd/" + type + ".xsd"));
        } catch (Exception brokenResource) {
            throw new IllegalStateException("ISO schema unavailable");
        }
    }

    private static Element document(String xml, String type) {
        SwiftProblem.require(xml != null && xml.length() <= 65536);
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            builder.setEntityResolver(
                    (publicId, systemId) -> {
                        throw new org.xml.sax.SAXException("External entity denied");
                    });
            var document = builder.parse(new InputSource(new StringReader(xml)));
            var validator = (PAYMENT.equals(type) ? PAYMENT_SCHEMA : STATUS_SCHEMA).newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validator.validate(new DOMSource(document));
            return document.getDocumentElement();
        } catch (Exception invalid) {
            throw SwiftProblem.invalid();
        }
    }

    private static Element child(Element parent, String name) {
        Element element = optional(parent, name);
        SwiftProblem.require(element != null);
        return element;
    }

    private static Element optional(Element parent, String name) {
        Element found = null;
        for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && name.equals(element.getLocalName())) {
                SwiftProblem.require(found == null);
                found = element;
            }
        }
        return found;
    }

    private static void allowed(Element parent, String... names) {
        var set = java.util.Set.of(names);
        for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element element)
                SwiftProblem.require(set.contains(element.getLocalName()));
    }

    private static String value(Element parent, String name) {
        return child(parent, name).getTextContent();
    }

    private static String accountValue(Element parent) {
        return value(child(child(parent, "Id"), "Othr"), "Id");
    }

    private static String account(String name, String value) {
        return "<" + name + "><Id><Othr>" + tag("Id", value) + "</Othr></Id></" + name + ">";
    }

    private static String tag(String name, String value) {
        return "<"
                + name
                + ">"
                + value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                + "</"
                + name
                + ">";
    }
}
