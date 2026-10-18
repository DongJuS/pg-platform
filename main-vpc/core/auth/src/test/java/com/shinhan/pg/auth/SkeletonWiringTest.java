package com.shinhan.pg.auth;

import com.shinhan.pg.auth.application.port.in.AuthenticateUseCase;
import com.shinhan.pg.auth.application.port.in.GetCurrentUserUseCase;
import com.shinhan.pg.auth.application.port.in.LogoutUseCase;
import com.shinhan.pg.auth.application.port.in.RefreshTokenUseCase;
import com.shinhan.pg.auth.application.port.in.RegisterUserUseCase;
import com.shinhan.pg.auth.application.port.in.VerifyOtpUseCase;
import com.shinhan.pg.auth.config.AuthSkeletonConfiguration;
import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                AuthSkeletonConfiguration.class,
                AuthenticateUseCase.class,
                RegisterUserUseCase.class,
                RefreshTokenUseCase.class,
                LogoutUseCase.class,
                GetCurrentUserUseCase.class,
                VerifyOtpUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assembles(
                AuthSkeletonConfiguration.class,
                AuthenticateUseCase.class,
                RegisterUserUseCase.class,
                RefreshTokenUseCase.class,
                LogoutUseCase.class,
                GetCurrentUserUseCase.class,
                VerifyOtpUseCase.class);
    }
}
