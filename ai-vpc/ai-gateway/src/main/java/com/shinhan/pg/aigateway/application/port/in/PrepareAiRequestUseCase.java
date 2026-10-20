package com.shinhan.pg.aigateway.application.port.in;

import com.shinhan.pg.aigateway.application.command.PrepareAiRequestCommand;
import com.shinhan.pg.aigateway.application.result.PrepareAiRequestResult;

public interface PrepareAiRequestUseCase {
    PrepareAiRequestResult prepare(PrepareAiRequestCommand command);
}
