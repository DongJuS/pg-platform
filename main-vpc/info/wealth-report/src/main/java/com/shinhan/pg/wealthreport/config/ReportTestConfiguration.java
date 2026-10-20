package com.shinhan.pg.wealthreport.config;

import com.shinhan.pg.wealthreport.application.port.out.AiReportDraftPort;
import com.shinhan.pg.wealthreport.domain.model.ReportDraft;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Explicit synthetic explanation profile for isolated tests. Uses real source/DB/PDF, never an LLM.
 */
@Configuration(proxyBeanMethods = false)
@Profile("report-test")
public class ReportTestConfiguration {
    @Bean
    public AiReportDraftPort syntheticReportDraft(org.springframework.core.env.Environment env) {
        if (env.acceptsProfiles(
                org.springframework.core.env.Profiles.of("aws", "production", "prod")))
            throw new IllegalStateException("report-test cannot run in production");
        return m ->
                new ReportDraft(
                        m.totalAssets().amount().toPlainString(),
                        m.cashIn().amount().toPlainString(),
                        m.cashOut().amount().toPlainString(),
                        "KRW",
                        m.asOf(),
                        m.calculationVersion(),
                        "선택 계좌의 잔액과 기간 중 현금흐름을 확인하세요. 계좌 사이의 이동도 포함합니다.",
                        "TEST_DOUBLE");
    }
}
