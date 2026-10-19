package com.shinhan.pg.auth.adapter.in.web;
import com.shinhan.pg.auth.application.command.ConsumeOtpCommand;
import com.shinhan.pg.auth.application.port.in.ConsumeOtpUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
/** Service-token protected endpoint. Never exposed by Gateway. */
@RestController @Profile("skeleton")
public final class InternalOtpController {
    private final ConsumeOtpUseCase useCase;
    public InternalOtpController(ConsumeOtpUseCase useCase) { this.useCase = useCase; }
    @PostMapping("/internal/v1/auth/otp/consume") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void consume(@RequestBody ConsumeOtpCommand command) { useCase.consumeOtp(command); }
}
