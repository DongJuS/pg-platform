package com.shinhan.pg.common.swiftmessages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/** Development USD/no-FX/zero-fee wire subset. Contains synthetic private data; never log it. */
public record Remittance(
        UUID transferId,
        String uetr,
        String reference,
        LocalDate valueDate,
        String senderName,
        String senderAccount,
        String recipientName,
        String recipientAccount,
        String bic,
        BigDecimal amount,
        String currency,
        String remittanceInformation,
        String chargeBearer) {
    public Remittance {
        SwiftProblem.require(transferId != null && valueDate != null);
        SwiftIds.uetr(uetr);
        text(reference, 16);
        text(senderName, 35);
        text(senderAccount, 36);
        text(recipientName, 35);
        text(recipientAccount, 34);
        SwiftProblem.require(bic != null && bic.matches("[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?"));
        SwiftProblem.require("USD".equals(currency) && amount != null && amount.signum() > 0);
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException invalid) {
            throw SwiftProblem.invalid();
        }
        SwiftProblem.require(amount.precision() <= 15 && "SHA".equals(chargeBearer));
        if (remittanceInformation != null) text(remittanceInformation, 140);
    }

    public static void text(String value, int maximum) {
        SwiftProblem.require(
                value != null
                        && !value.isBlank()
                        && value.length() <= maximum
                        && value.chars().allMatch(c -> c >= 32 && c <= 126)
                        && !value.contains(":"));
    }

    @Override
    public String toString() {
        return "Remittance[REDACTED]";
    }
}
