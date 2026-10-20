package com.shinhan.pg.common.swiftmessages;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * Explicit block-4 subset, not a FIN envelope parser. Duplicate/unknown/out-of-order fields
 * rejected.
 */
public final class Mt103Subset {
    private static final List<String> ORDER =
            List.of("20", "23B", "32A", "50K", "57A", "59", "70", "71A");

    private Mt103Subset() {}

    public static Remittance parse(UUID transferId, String uetr, String raw) {
        SwiftProblem.require(raw != null && raw.length() <= 4096);
        raw = raw.replace("\r\n", "\n");
        SwiftProblem.require(raw.startsWith("{4:\n") && raw.endsWith("\n-}"));
        var fields = new LinkedHashMap<String, String>();
        String current = null;
        int last = -1;
        for (String line : raw.substring(4, raw.length() - 3).split("\n", -1)) {
            if (line.startsWith(":")) {
                int end = line.indexOf(':', 1);
                SwiftProblem.require(end > 1);
                current = line.substring(1, end);
                int order = ORDER.indexOf(current);
                SwiftProblem.require(order > last && !fields.containsKey(current));
                last = order;
                fields.put(current, line.substring(end + 1));
            } else {
                SwiftProblem.require(current != null && !line.isEmpty());
                fields.put(current, fields.get(current) + "\n" + line);
            }
        }
        SwiftProblem.require(
                fields.keySet().containsAll(List.of("20", "23B", "32A", "50K", "57A", "59", "71A"))
                        && "CRED".equals(fields.get("23B")));
        try {
            String settlement = fields.get("32A");
            SwiftProblem.require(settlement.matches("[0-9]{6}USD[0-9]{1,13},[0-9]{2}"));
            var date =
                    LocalDate.parse(
                            "20" + settlement.substring(0, 6),
                            DateTimeFormatter.ofPattern("uuuuMMdd")
                                    .withResolverStyle(ResolverStyle.STRICT));
            var sender = party(fields.get("50K"));
            var receiver = party(fields.get("59"));
            return new Remittance(
                    transferId,
                    uetr,
                    fields.get("20"),
                    date,
                    sender[1],
                    sender[0],
                    receiver[1],
                    receiver[0],
                    fields.get("57A"),
                    new BigDecimal(settlement.substring(9).replace(',', '.')),
                    "USD",
                    fields.get("70"),
                    fields.get("71A"));
        } catch (RuntimeException invalid) {
            throw SwiftProblem.invalid();
        }
    }

    private static String[] party(String value) {
        String[] parts = value.split("\n", -1);
        SwiftProblem.require(
                parts.length == 2 && parts[0].startsWith("/") && parts[0].length() > 1);
        return new String[] {parts[0].substring(1), parts[1]};
    }

    public static String write(Remittance r) {
        return "{4:\n:20:"
                + r.reference()
                + "\n:23B:CRED\n:32A:"
                + r.valueDate().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "USD"
                + r.amount().toPlainString().replace('.', ',')
                + "\n:50K:/"
                + r.senderAccount()
                + "\n"
                + r.senderName()
                + "\n:57A:"
                + r.bic()
                + "\n:59:/"
                + r.recipientAccount()
                + "\n"
                + r.recipientName()
                + (r.remittanceInformation() == null ? "" : "\n:70:" + r.remittanceInformation())
                + "\n:71A:SHA\n-}";
    }
}
