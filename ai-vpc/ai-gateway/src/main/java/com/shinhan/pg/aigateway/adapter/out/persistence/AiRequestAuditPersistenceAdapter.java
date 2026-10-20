package com.shinhan.pg.aigateway.adapter.out.persistence;

import com.shinhan.pg.aigateway.application.port.out.AiAuditWritePort;
import com.shinhan.pg.aigateway.application.port.out.AiRequestAuditRepository;
import com.shinhan.pg.aigateway.adapter.out.persistence.entity.AiRequestAuditEntity;
import com.shinhan.pg.aigateway.adapter.out.persistence.mapper.AiRequestAuditEntityMapper;
import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

/** One independently committed insert; no prompt, hash or mapping ever reaches JDBC. */
public final class AiRequestAuditPersistenceAdapter implements AiRequestAuditRepository, AiAuditWritePort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final AiRequestAuditEntityMapper mapper = new AiRequestAuditEntityMapper();
    public AiRequestAuditPersistenceAdapter(DataSource source) {
        jdbc = new JdbcTemplate(source);
        tx = new TransactionTemplate(new DataSourceTransactionManager(source));
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setTimeout(5);
        jdbc.setQueryTimeout(5);
    }
    @Override public Optional<AiRequestAudit> findById(UUID id) {
        return jdbc.query("select * from ai_request_audits where request_id=?", (rs, index) ->
                mapper.toDomain(new AiRequestAuditEntity(rs.getObject("request_id", UUID.class),
                        rs.getString("caller_service_id"), rs.getString("decision"), rs.getString("reason"),
                        rs.getInt("name_count"), rs.getInt("rrn_count"), rs.getInt("account_count"),
                        rs.getInt("phone_count"), rs.getInt("email_count"), rs.getInt("card_count"), rs.getString("policy_version"),
                        rs.getTimestamp("created_at").toInstant())), id).stream().findFirst();
    }
    @Override public void save(AiRequestAudit value) { record(value); }
    @Override public void record(AiRequestAudit audit) {
        var r = mapper.toEntity(audit);
        tx.executeWithoutResult(status -> jdbc.update("""
                insert into ai_request_audits
                (request_id,caller_service_id,decision,reason,name_count,rrn_count,account_count,
                 phone_count,email_count,card_count,policy_version,created_at) values (?,?,?,?,?,?,?,?,?,?,?,?)
                """, r.requestId(), r.callerServiceId(), r.decision(), r.reason(), r.nameCount(),
                r.rrnCount(), r.accountCount(), r.phoneCount(), r.emailCount(), r.cardCount(), r.policyVersion(),
                Timestamp.from(r.createdAt())));
    }
}
