package com.shinhan.pg.auth.application.port.in;
import com.shinhan.pg.auth.application.command.ConsumeOtpCommand;
public interface ConsumeOtpUseCase { void consumeOtp(ConsumeOtpCommand command); }
