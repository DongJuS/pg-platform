package com.shinhan.pg.common.archrules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** 스켈레톤이 기본 비활성이며, 유효한 요청에도 가짜 성공 대신 501을 반환하는지 확인한다. */
public final class ApiSkeletonAssertions {
    private ApiSkeletonAssertions() {}

    public record Call(String id, String method, String path, String body, String query) {}

    public static void verify(
            Class<?> configuration,
            Class<?>[] controllers,
            Object advice,
            List<Call> calls,
            String malformedBodyPath,
            String invalidIdPath)
            throws Exception {
        try (var inactive = new AnnotationConfigApplicationContext()) {
            inactive.register(configuration);
            inactive.register(controllers);
            inactive.refresh();
            for (var type : controllers)
                assertTrue(
                        inactive.getBeansOfType(type).isEmpty(),
                        type.getName() + " must stay inactive by default");
        }
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("skeleton");
            context.register(configuration);
            context.register(controllers);
            context.refresh();
            var beans = Arrays.stream(controllers).map(context::getBean).toArray();
            var mvc = MockMvcBuilders.standaloneSetup(beans).setControllerAdvice(advice).build();
            for (var call : calls) {
                var request =
                        request(HttpMethod.valueOf(call.method()), call.path())
                                .requestAttr(
                                        "actorUserId",
                                        UUID.fromString("00000000-0000-0000-0000-000000000001"))
                                .requestAttr("callerServiceId", "mock-contract-test")
                                .header("Idempotency-Key", "mock-contract-key")
                                .header("Authorization", "Bearer mock-token")
                                .header("X-PG-Signature", "mock-signature");
                if (call.body() != null)
                    request.contentType(MediaType.APPLICATION_JSON).content(call.body());
                if (call.query() != null && !call.query().isEmpty())
                    for (var pair : call.query().split("&")) {
                        var kv = pair.split("=", 2);
                        request.param(kv[0], kv[1]);
                    }
                try {
                    mvc.perform(request)
                            .andExpect(status().isNotImplemented())
                            .andExpect(jsonPath("$.code").value("COMMON-NOT-IMPLEMENTED"));
                } catch (AssertionError error) {
                    throw new AssertionError(
                            call.id() + " " + call.method() + " " + call.path(), error);
                }
            }
            if (malformedBodyPath != null)
                mvc.perform(
                                post(malformedBodyPath)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{broken")
                                        .header("Idempotency-Key", "mock-key"))
                        .andExpect(status().isBadRequest());
            if (invalidIdPath != null)
                mvc.perform(get(invalidIdPath)).andExpect(status().isBadRequest());
        }
    }

    public static void heldApisHaveNoMappings(Class<?>... controllers) {
        for (var type : controllers) {
            assertFalse(
                    AnnotatedElementUtils.hasAnnotation(type, RestController.class),
                    type.getName());
            for (var method : type.getDeclaredMethods())
                assertFalse(
                        AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class),
                        method.toString());
        }
    }
}
