package com.shinhan.pg.common.amlcontracts;

import com.shinhan.pg.common.commonerror.ErrorType;

public final class AmlProblem extends RuntimeException {
    private final String code;
    private final ErrorType type;

    public AmlProblem(String code, ErrorType type) {
        super("AML request could not be completed");
        this.code = code;
        this.type = type;
    }

    public String code() {
        return code;
    }

    public ErrorType type() {
        return type;
    }

    public static AmlProblem invalid() {
        return new AmlProblem("AML-INVALID", ErrorType.INVALID);
    }

    public static AmlProblem conflict() {
        return new AmlProblem("AML-CONTENT-CONFLICT", ErrorType.CONFLICT);
    }

    public static AmlProblem missing() {
        return new AmlProblem("AML-NOT-FOUND", ErrorType.NOT_FOUND);
    }

    public static void require(boolean ok) {
        if (!ok) throw invalid();
    }
}
