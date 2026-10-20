package com.shinhan.pg.common.swiftmessages;

import com.shinhan.pg.common.commonerror.ErrorType;

/** Fixed errors deliberately exclude messages, accounts, credentials and parser diagnostics. */
public final class SwiftProblem extends RuntimeException {
    private final String code;
    private final ErrorType type;

    public SwiftProblem(String code, ErrorType type) {
        super("SWIFT request " + code);
        this.code = "SWIFT-" + code;
        this.type = type;
    }

    public String code() {
        return code;
    }

    public ErrorType type() {
        return type;
    }

    public static SwiftProblem invalid() {
        return new SwiftProblem("INVALID", ErrorType.INVALID);
    }

    public static SwiftProblem conflict() {
        return new SwiftProblem("CONFLICT", ErrorType.CONFLICT);
    }

    public static SwiftProblem unavailable() {
        return new SwiftProblem("UNAVAILABLE", ErrorType.EXTERNAL_FAILURE);
    }

    public static void require(boolean condition) {
        if (!condition) throw invalid();
    }
}
