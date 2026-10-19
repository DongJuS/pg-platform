package com.shinhan.pg.transfer;

import com.shinhan.pg.transfer.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.transfer.adapter.in.web.TransferController;
import com.shinhan.pg.transfer.adapter.in.web.TransferExceptionHandler;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferHttpErrorsTest {
    @Test void insufficientFundsIs422WhenBothAdviceBeansAreRegistered() throws Exception {
        var controller = new TransferController(command -> {
            throw new TransferException(TransferErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds");
        }, command -> null, command -> null);
        var mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new PlatformExceptionHandler(), new TransferExceptionHandler()).build();
        mvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", "test-key")
            .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
            .content("{\"fromAccountId\":\"00000000-0000-0000-0000-000000000002\",\"toAccountId\":\"00000000-0000-0000-0000-000000000003\",\"amount\":1000,\"currency\":\"KRW\",\"otpToken\":\"test\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("TRANSFER-INSUFFICIENT-FUNDS"));
    }
    @Test void duplicateRequestReturns409AndItsExistingId() throws Exception {
        var id = java.util.UUID.randomUUID();
        var controller = new TransferController(command -> {
            throw new TransferException(TransferErrorCode.DUPLICATE_REQUEST, "Duplicate", id);
        }, command -> null, command -> null);
        var mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new PlatformExceptionHandler(), new TransferExceptionHandler()).build();
        mvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON)
            .header("Idempotency-Key", "same-key")
            .header("X-User-Id", "00000000-0000-0000-0000-000000000001")
            .content("{\"fromAccountId\":\"00000000-0000-0000-0000-000000000002\",\"toAccountId\":\"00000000-0000-0000-0000-000000000003\",\"amount\":1000,\"currency\":\"KRW\",\"otpToken\":\"test\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.existingTransferId").value(id.toString()));
    }
}
