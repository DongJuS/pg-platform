package com.shinhan.pg.auth;

import com.shinhan.pg.auth.adapter.in.web.AuthController;
import com.shinhan.pg.auth.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.auth.adapter.in.web.UserProfileController;
import com.shinhan.pg.auth.config.AuthSkeletonConfiguration;
import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                AuthSkeletonConfiguration.class,
                new Class<?>[] {AuthController.class, UserProfileController.class},
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F01",
                                "POST",
                                "/api/v1/auth/register",
                                "{\"email\": \"example@example.invalid\", \"password\":"
                                    + " \"mock-value\", \"name\": \"mock-value\", \"phoneNumber\":"
                                    + " \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F02",
                                "POST",
                                "/api/v1/auth/login",
                                "{\"email\": \"example@example.invalid\", \"password\":"
                                        + " \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F03",
                                "POST",
                                "/api/v1/auth/refresh",
                                "{\"refreshToken\": \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F04", "POST", "/api/v1/auth/logout", null, ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F05", "GET", "/api/v1/users/me", null, ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP1-F06",
                                "POST",
                                "/api/v1/auth/otp/verify",
                                "{\"transferDraftId\": \"00000000-0000-0000-0000-000000000002\","
                                        + " \"otpCode\": \"mock-value\"}",
                                "")),
                "/api/v1/auth/register",
                null);
    }
}
