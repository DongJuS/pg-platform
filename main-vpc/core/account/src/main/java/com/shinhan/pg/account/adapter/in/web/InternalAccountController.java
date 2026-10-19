package com.shinhan.pg.account.adapter.in.web;
import com.shinhan.pg.account.application.port.in.InspectAccountUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
/** GET /internal/v1/accounts/{accountId}; protected by InternalServiceAuthenticationFilter. */
@RestController @Profile("skeleton")
public final class InternalAccountController {
    private final InspectAccountUseCase accounts;
    public InternalAccountController(InspectAccountUseCase accounts) { this.accounts = accounts; }
    @GetMapping("/internal/v1/accounts/{accountId}")
    public AccountAccessResponse inspect(@PathVariable UUID accountId) {
        var account = accounts.inspectAccount(accountId);
        return new AccountAccessResponse(account.accountId(), account.ownerId(), account.currency(), account.status());
    }
    public record AccountAccessResponse(UUID accountId, UUID ownerId, String currency, String status) {}
}
