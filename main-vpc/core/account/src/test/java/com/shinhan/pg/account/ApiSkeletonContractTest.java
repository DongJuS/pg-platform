package com.shinhan.pg.account;

import com.shinhan.pg.account.adapter.in.web.AccountController;
import com.shinhan.pg.account.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.account.application.port.in.ChangeAccountStatusUseCase;
import com.shinhan.pg.account.application.port.in.GetAccountUseCase;
import com.shinhan.pg.account.application.port.in.ListAccountsUseCase;
import com.shinhan.pg.account.application.port.in.OpenAccountUseCase;
import com.shinhan.pg.account.config.AccountSkeletonConfiguration;
import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;

import org.junit.jupiter.api.Test;

/**
 * [MVP1-F07..F10] account는 구현이 끝난 서비스다. 유효 요청의 업무 동작은 단위 테스트와
 * AccountPersistenceIT(-Pintegration=true)로 대체하고 여기서는 기본 비활성 + 형식 오류 400 계약만 확인한다.
 */
class ApiSkeletonContractTest {
    @Test
    void endpointsStayInactiveByDefaultAndRejectMalformedRequests() throws Exception {
        ApiSkeletonAssertions.implementedEndpoints(
                AccountSkeletonConfiguration.class,
                new Object[] {
                    new AccountController(
                            unsupported(OpenAccountUseCase.class),
                            unsupported(ListAccountsUseCase.class),
                            unsupported(GetAccountUseCase.class),
                            unsupported(ChangeAccountStatusUseCase.class))
                },
                new PlatformExceptionHandler(),
                "/api/v1/accounts",
                "/api/v1/accounts/invalid-id");
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
