package com.shinhan.pg.wealthreport.adapter.out.execution;

import com.shinhan.pg.wealthreport.application.port.out.GenerationExecutionPort;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * No queue: an uninterruptible dependency retains its worker and cannot spawn unlimited
 * replacements.
 */
public final class BoundedGenerationExecutor implements GenerationExecutionPort, AutoCloseable {
    private final ThreadPoolExecutor pool;

    public BoundedGenerationExecutor(int capacity) {
        pool =
                new ThreadPoolExecutor(
                        capacity,
                        capacity,
                        0,
                        TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        r -> {
                            var t = new Thread(r, "report-stage");
                            t.setDaemon(true);
                            return t;
                        });
    }

    public <T> T run(Callable<T> action, Duration budget) {
        Future<T> f;
        try {
            f = pool.submit(action);
        } catch (RejectedExecutionException e) {
            throw new ReportException(WealthReportErrorCode.GENERATION_BUSY);
        }
        try {
            return f.get(Math.max(1, budget.toNanos()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            throw new ReportException(WealthReportErrorCode.GENERATION_TIMEOUT);
        } catch (InterruptedException e) {
            f.cancel(true);
            Thread.currentThread().interrupt();
            throw new ReportException(WealthReportErrorCode.INTERRUPTED);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new ReportException(WealthReportErrorCode.SOURCE_UNAVAILABLE);
        }
    }

    public void close() {
        pool.shutdownNow();
    }
}
