package com.shinhan.pg.ledger.config;

import com.shinhan.pg.ledger.adapter.out.persistence.JournalAuditAdapter;
import com.shinhan.pg.ledger.application.port.in.GetJournalAuditUseCase;
import com.shinhan.pg.ledger.application.service.JournalAuditService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class JournalAuditConfiguration {
    @Bean
    GetJournalAuditUseCase journalAudit(DataSource source) {
        return new JournalAuditService(new JournalAuditAdapter(new JdbcTemplate(source)));
    }
}
