package com.shinhan.pg.transfer;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.transfer.adapter.in.web.OverseasTransferController;
import com.shinhan.pg.transfer.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.transfer.adapter.in.web.TransferController;
import com.shinhan.pg.transfer.application.port.in.GetTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.ListTransfersUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestTransferUseCase;
import com.shinhan.pg.transfer.config.TransferSkeletonConfiguration;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * WO-A6: MVP1-F15~F17은 구현이 끝난 endpoint다. 유효 요청의 업무 동작은 TransferFlowIT·
 * TransferApplicationServiceTest로 대체하고 여기서는 기본 비활성 + 형식 오류 400 계약만 확인한다.
 * 해외 송금(MVP5-F01/F02)은 이번 범위가 아니므로 501 계약을 유지한다.
 */
class ApiSkeletonContractTest {
    @Test
    void implementedEndpointsStayInactiveByDefaultAndRejectMalformedRequests() throws Exception {
        ApiSkeletonAssertions.implementedEndpoints(
                TransferSkeletonConfiguration.class,
                new Object[] {
                    new TransferController(
                            unsupported(RequestTransferUseCase.class),
                            unsupported(GetTransferUseCase.class),
                            unsupported(ListTransfersUseCase.class))
                },
                new PlatformExceptionHandler(),
                "/api/v1/transfers",
                "/api/v1/transfers/invalid-id");
    }

    @Test
    void deferredOverseasEndpointsStay501() throws Exception {
        ApiSkeletonAssertions.verify(
                TransferSkeletonConfiguration.class,
                new Class<?>[] {OverseasTransferController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP5-F01",
                                "POST",
                                "/api/v1/transfers/overseas",
                                "{\"fromAccountId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"recipientName\": \"mock-value\", \"recipientAccount\":"
                                    + " \"mock-value\", \"bic\": \"mock-value\", \"amount\": 1000,"
                                    + " \"currency\": \"KRW\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP5-F02",
                                "GET",
                                "/api/v1/transfers/overseas/00000000-0000-0000-0000-000000000002",
                                null,
                                "")),
                "/api/v1/transfers/overseas",
                "/api/v1/transfers/overseas/invalid-id");
    }

    private static <T> T unsupported(Class<T> type) {
        java.lang.reflect.InvocationHandler handler =
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class)
                        return switch (method.getName()) {
                            case "toString" -> "contract-test-stub";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    throw new UnsupportedOperationException("contract test stub");
                };
        return type.cast(
                java.lang.reflect.Proxy.newProxyInstance(
                        type.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
