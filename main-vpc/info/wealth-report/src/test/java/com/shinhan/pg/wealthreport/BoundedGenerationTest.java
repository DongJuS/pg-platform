package com.shinhan.pg.wealthreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.wealthreport.adapter.out.execution.BoundedGenerationExecutor;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class BoundedGenerationTest {
    @Test
    void timeoutDoesNotReleaseAnUninterruptibleWorkerIntoUnboundedReplacement() throws Exception {
        var release = new CountDownLatch(1);
        var started = new CountDownLatch(1);
        try (var executor = new BoundedGenerationExecutor(1)) {
            assertEquals(
                    WealthReportErrorCode.GENERATION_TIMEOUT,
                    assertThrows(
                                    ReportException.class,
                                    () ->
                                            executor.run(
                                                    () -> {
                                                        started.countDown();
                                                        while (release.getCount() > 0) {
                                                            try {
                                                                release.await();
                                                            } catch (InterruptedException ignored) {
                                                            }
                                                        }
                                                        return true;
                                                    },
                                                    Duration.ofMillis(50)))
                            .code());
            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertEquals(
                    WealthReportErrorCode.GENERATION_BUSY,
                    assertThrows(
                                    ReportException.class,
                                    () -> executor.run(() -> true, Duration.ofSeconds(1)))
                            .code());
            release.countDown();
        } finally {
            release.countDown();
        }
    }
}
