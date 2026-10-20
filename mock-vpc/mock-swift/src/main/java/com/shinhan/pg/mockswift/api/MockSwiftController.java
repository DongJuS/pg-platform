package com.shinhan.pg.mockswift.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.swiftmessages.Iso20022;
import com.shinhan.pg.common.swiftmessages.PrivatePayload;
import com.shinhan.pg.common.swiftmessages.SwiftIds;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Durable local institution simulator. One UETR + one byte hash has at most one settlement effect.
 * Files are private to the disposable Mock volume; no real bank destination or SWIFT transport
 * exists.
 */
@RestController
@org.springframework.scheduling.annotation.EnableScheduling
@Profile("swift-test")
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class MockSwiftController {
    private final ObjectMapper json;
    private final Path directory;
    private final PrivatePayload privacy;
    private final String fepToken;
    private final String operationsToken;

    public MockSwiftController(ObjectMapper json, Environment env) {
        this.json = json;
        directory =
                Path.of(env.getProperty("pg.swift.mock-directory", "/tmp/mock-swift-state"))
                        .toAbsolutePath();
        privacy = new PrivatePayload(env.getProperty("pg.swift.payload-key", ""));
        fepToken = env.getProperty("pg.swift.mock-token", "");
        operationsToken = env.getProperty("pg.swift.caller-tokens.swift-operations", "");
        try {
            Files.createDirectories(directory);
            Files.setPosixFilePermissions(
                    directory,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        } catch (Exception e) {
            throw new IllegalStateException("Mock storage unavailable");
        }
    }

    @PostMapping("/mock/v1/swift/messages")
    public Receipt send(
            @RequestHeader("X-Internal-Caller") String caller,
            @RequestHeader("X-Internal-Service-Token") String token,
            @RequestBody Request request) {
        authorize(caller, token, false);
        SwiftIds.uetr(request.uetr());
        var r = Iso20022.parsePayment(request.transferId(), request.pacs008());
        if (!r.uetr().equals(request.uetr())) throw SwiftProblem.conflict();
        State state =
                locked(
                        request.uetr(),
                        () -> {
                            State old = read(request.uetr());
                            String hash = SwiftIds.hash(request.pacs008());
                            if (old != null
                                    && (!old.hash().equals(hash)
                                            || !old.transferId().equals(request.transferId())))
                                throw SwiftProblem.conflict();
                            String mode =
                                    r.recipientAccount().startsWith("MOCK_")
                                            ? r.recipientAccount()
                                            : "MOCK_SUCCESS";
                            if (old == null)
                                old =
                                        new State(
                                                request.transferId(),
                                                request.uetr(),
                                                r.reference(),
                                                hash,
                                                privacy.encrypt(request.uetr(), request.pacs008()),
                                                mode,
                                                0,
                                                0,
                                                null,
                                                Instant.now(),
                                                Instant.now(),
                                                0);
                            int attempts = old.attempts() + 1;
                            int effects = old.effects();
                            String code = old.code();
                            if (code == null
                                    && !"MOCK_UNKNOWN".equals(mode)
                                    && (!"MOCK_RETRY".equals(mode) || attempts > 1)) {
                                code =
                                        "MOCK_REJECT".equals(mode)
                                                ? "RJCT"
                                                : ("MOCK_PENDING".equals(mode)
                                                                || "MOCK_DELAY".equals(mode)
                                                        ? "ACSP"
                                                        : "ACSC");
                                if ("ACSC".equals(code)) effects = 1;
                            }
                            State next =
                                    new State(
                                            old.transferId(),
                                            old.uetr(),
                                            old.reference(),
                                            old.hash(),
                                            old.privateXml(),
                                            mode,
                                            attempts,
                                            effects,
                                            code,
                                            old.createdAt(),
                                            old.changedAt(),
                                            old.version());
                            write(next);
                            return next;
                        });
        if ("MOCK_LOST_RESPONSE".equals(state.mode())) {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw SwiftProblem.unavailable();
        }
        if (state.code() == null) throw SwiftProblem.unavailable();
        return new Receipt(request.uetr(), "ACCEPTED");
    }

    @GetMapping("/mock/v1/swift/messages/{uetr}/result")
    public Result query(
            @RequestHeader("X-Internal-Caller") String caller,
            @RequestHeader("X-Internal-Service-Token") String token,
            @PathVariable String uetr) {
        authorize(caller, token, false);
        SwiftIds.uetr(uetr);
        return locked(
                uetr,
                () -> {
                    State state = read(uetr);
                    if (state == null || state.code() == null)
                        throw new SwiftProblem("NOT_FOUND", ErrorType.NOT_FOUND);
                    if ("MOCK_DELAY".equals(state.mode())
                            && Instant.now().isAfter(state.createdAt().plusSeconds(2))
                            && "ACSP".equals(state.code())) {
                        state =
                                new State(
                                        state.transferId(),
                                        uetr,
                                        state.reference(),
                                        state.hash(),
                                        state.privateXml(),
                                        state.mode(),
                                        state.attempts(),
                                        1,
                                        "ACSC",
                                        state.createdAt(),
                                        Instant.now(),
                                        state.version() + 1);
                        write(state);
                    }
                    return new Result(
                            Iso20022.status(
                                    new Iso20022.Result(
                                            state.reference()
                                                    + "-"
                                                    + state.version()
                                                    + "-"
                                                    + state.code(),
                                            state.reference(),
                                            uetr,
                                            state.code()),
                                    state.changedAt()));
                });
    }

    @GetMapping("/mock/v1/swift/messages/{uetr}")
    public Map<String, Object> inspect(
            @RequestHeader("X-Internal-Caller") String caller,
            @RequestHeader("X-Internal-Service-Token") String token,
            @PathVariable String uetr) {
        authorize(caller, token, true);
        SwiftIds.uetr(uetr);
        return locked(
                uetr,
                () -> {
                    var s = read(uetr);
                    if (s == null) throw SwiftProblem.invalid();
                    return Map.of(
                            "uetr",
                            uetr,
                            "attempts",
                            s.attempts(),
                            "financialEffects",
                            s.effects(),
                            "hash",
                            s.hash());
                });
    }

    @PostMapping("/mock/v1/swift/messages/{uetr}/results")
    public void change(
            @RequestHeader("X-Internal-Caller") String caller,
            @RequestHeader("X-Internal-Service-Token") String token,
            @PathVariable String uetr,
            @RequestBody Change request) {
        authorize(caller, token, true);
        SwiftIds.uetr(uetr);
        SwiftProblem.require(
                java.util.Set.of("ACSC", "RJCT", "ACSP", "PDNG").contains(request.code()));
        locked(
                uetr,
                () -> {
                    var s = read(uetr);
                    if (s == null) throw SwiftProblem.invalid();
                    write(
                            new State(
                                    s.transferId(),
                                    uetr,
                                    s.reference(),
                                    s.hash(),
                                    s.privateXml(),
                                    s.mode(),
                                    s.attempts(),
                                    "ACSC".equals(request.code()) ? 1 : s.effects(),
                                    request.code(),
                                    s.createdAt(),
                                    Instant.now(),
                                    s.version() + 1));
                    return null;
                });
    }

    private synchronized <T> T locked(String uetr, java.util.function.Supplier<T> action) {
        try (var channel =
                        FileChannel.open(
                                directory.resolve("state.lock"),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                var ignored = channel.lock()) {
            return action.get();
        } catch (SwiftProblem business) {
            throw business;
        } catch (Exception e) {
            throw SwiftProblem.unavailable();
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
    public void purge() {
        try (var files = Files.list(directory)) {
            files.filter(p -> p.getFileName().toString().matches("[0-9a-f-]{36}\\.json"))
                    .forEach(
                            p -> {
                                String u = p.getFileName().toString().substring(0, 36);
                                locked(
                                        u,
                                        () -> {
                                            read(u);
                                            return null;
                                        });
                            });
        } catch (java.io.IOException unavailable) {
            throw SwiftProblem.unavailable();
        }
    }

    private State read(String uetr) {
        Path path = directory.resolve(uetr + ".json");
        if (!Files.exists(path)) return null;
        try {
            State s = json.readValue(Files.readAllBytes(path), State.class);
            if (s.privateXml() != null
                    && Instant.now().isAfter(s.createdAt().plusSeconds(172800))) {
                s =
                        new State(
                                s.transferId(),
                                s.uetr(),
                                s.reference(),
                                s.hash(),
                                null,
                                s.mode(),
                                s.attempts(),
                                s.effects(),
                                s.code(),
                                s.createdAt(),
                                s.changedAt(),
                                s.version());
                write(s);
            }
            return s;
        } catch (Exception e) {
            throw SwiftProblem.unavailable();
        }
    }

    private void write(State state) {
        try {
            Path temporary = Files.createTempFile(directory, "state-", ".tmp");
            byte[] bytes = json.writeValueAsBytes(state);
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(
                    temporary,
                    directory.resolve(state.uetr() + ".json"),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            try (var channel = FileChannel.open(directory, StandardOpenOption.READ)) {
                channel.force(true);
            }
        } catch (Exception e) {
            throw SwiftProblem.unavailable();
        }
    }

    private void authorize(String caller, String supplied, boolean operations) {
        String expected = operations ? operationsToken : fepToken;
        if (!(operations ? "swift-operations" : "fep-adapter").equals(caller)
                || expected.isBlank()
                || !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8)))
            throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
    }

    public record Request(UUID transferId, String uetr, String pacs008) {
        @Override
        public String toString() {
            return "MockSwiftRequest[REDACTED]";
        }
    }

    public record Receipt(String uetr, String status) {}

    public record Result(String pacs002) {
        @Override
        public String toString() {
            return "MockSwiftResult[REDACTED]";
        }
    }

    public record Change(String code) {}

    private record State(
            UUID transferId,
            String uetr,
            String reference,
            String hash,
            String privateXml,
            String mode,
            int attempts,
            int effects,
            String code,
            Instant createdAt,
            Instant changedAt,
            int version) {
        @Override
        public String toString() {
            return "MockSwiftState[REDACTED]";
        }
    }
}
