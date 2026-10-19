package com.shinhan.pg.transfer.application.port.out;
import com.shinhan.pg.transfer.application.readmodel.AccountAccessSnapshot;
import java.util.UUID;
public interface AccountQueryPort { AccountAccessSnapshot getAccount(UUID accountId); }
