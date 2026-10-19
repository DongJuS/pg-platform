package com.shinhan.pg.account.application.port.in;
import com.shinhan.pg.account.application.result.GetAccountResult;
import java.util.UUID;
/** Internal-only lookup. Its inbound adapter requires service authentication. */
public interface InspectAccountUseCase { GetAccountResult inspectAccount(UUID accountId); }
