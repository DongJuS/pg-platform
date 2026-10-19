package com.shinhan.pg.ledger.application.port.out;
import com.shinhan.pg.ledger.application.readmodel.AccountAccessSnapshot;
import java.util.UUID;
public interface AccountQueryPort { AccountAccessSnapshot getAccount(UUID accountId); }
