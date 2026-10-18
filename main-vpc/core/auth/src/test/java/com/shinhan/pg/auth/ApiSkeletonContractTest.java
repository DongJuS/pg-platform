package com.shinhan.pg.auth;

import com.shinhan.pg.auth.adapter.in.web.AuthController;
import com.shinhan.pg.auth.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.auth.adapter.in.web.UserProfileController;
import com.shinhan.pg.auth.application.port.in.AuthenticateUseCase;
import com.shinhan.pg.auth.application.port.in.GetCurrentUserUseCase;
import com.shinhan.pg.auth.application.port.in.LogoutUseCase;
import com.shinhan.pg.auth.application.port.in.RefreshTokenUseCase;
import com.shinhan.pg.auth.application.port.in.RegisterUserUseCase;
import com.shinhan.pg.auth.application.port.in.VerifyOtpUseCase;
import com.shinhan.pg.auth.config.AuthSkeletonConfiguration;
import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;

import org.junit.jupiter.api.Test;

/**
 * [MVP1-F01..F06] auth는 구현이 끝난 서비스다. 유효 요청의 업무 동작은 AuthApiIntegrationTest로
 * 대체하고 여기서는 기본 비활성 + 형식 오류 400 계약만 확인한다.
 */
class ApiSkeletonContractTest {
    @Test
    void endpointsStayInactiveByDefaultAndRejectMalformedRequests() throws Exception {
        ApiSkeletonAssertions.implementedEndpoints(
                AuthSkeletonConfiguration.class,
                new Object[] {
                    new AuthController(
                            unsupported(RegisterUserUseCase.class),
                            unsupported(AuthenticateUseCase.class),
                            unsupported(RefreshTokenUseCase.class),
                            unsupported(LogoutUseCase.class),
                            unsupported(VerifyOtpUseCase.class)),
                    new UserProfileController(unsupported(GetCurrentUserUseCase.class))
                },
                new PlatformExceptionHandler(),
                "/api/v1/auth/register",
                null);
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
