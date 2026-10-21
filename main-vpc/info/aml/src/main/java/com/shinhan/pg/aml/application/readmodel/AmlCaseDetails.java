package com.shinhan.pg.aml.application.readmodel;

import com.shinhan.pg.aml.domain.model.AmlCase;
import com.shinhan.pg.aml.domain.model.CaseAssessment;

public record AmlCaseDetails(
        AmlCase summary, CaseAssessment originalAssessment, CaseAssessment latestAssessment) {}
