package com.shinhan.pg.wealthreport.application.port.out;

import java.time.Duration;
import java.util.concurrent.Callable;

/** Bounded stage execution. Cancellation never implies a remote model was not charged. */
public interface GenerationExecutionPort {
    <T> T run(Callable<T> action, Duration budget);
}
