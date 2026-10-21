package com.shinhan.pg.mockkofiu.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.commonerror.ErrorType;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
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
import java.util.Set;
import java.util.UUID;

/**
 * Local simulator, durable one-key/one-content acceptance. Fault modes are configured only through
 * a separate test credential.
 */
@org.springframework.scheduling.annotation.EnableScheduling
@RestController
@Profile("aml-test")
@ConditionalOnProperty(name = "pg.aml.enabled", havingValue = "true")
public final class MockKofiuController {
    private final ObjectMapper json;
    private final Path directory;
    private final String token, operations;
    private final Object mutex = new Object();

    public MockKofiuController(ObjectMapper json, Environment e) {
        this.json = json;
        token = e.getProperty("pg.aml.mock-token", "");
        operations = e.getProperty("pg.aml.mock-operations-token", "");
        var profiles = Set.of(e.getActiveProfiles());
        if (profiles.contains("aws")
                || profiles.contains("prod")
                || profiles.contains("production")) throw new IllegalStateException("Mock only");
        directory =
                Path.of(e.getProperty("pg.aml.mock-directory", "/tmp/mock-kofiu-state"))
                        .toAbsolutePath();
        try {
            Files.createDirectories(directory);
            Files.setPosixFilePermissions(
                    directory,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        } catch (Exception ex) {
            throw new IllegalStateException("Mock storage unavailable");
        }
    }

    @PostMapping("/mock/v1/kofiu/reports")
    public KofiuResult send(
            @RequestHeader("X-Kofiu-Token") String credential, @RequestBody AmlReportDocument d) {
        authorize(credential, token);
        State state =
                locked(
                        d.reportKey(),
                        () -> {
                            if (Files.exists(
                                    directory.resolve(
                                            com.shinhan.pg.common.amlcontracts.AmlHashes.hash(
                                                            d.reportKey())
                                                    + ".expired"))) throw AmlProblem.conflict();
                            State old = read(d.reportKey());
                            if (old != null
                                    && (!d.hash().equals(old.hash())
                                            || !d.reportId().equals(old.reportId())))
                                throw AmlProblem.conflict();
                            String mode = mode(d.reportKey());
                            int deliveries = old == null ? 1 : old.deliveries() + 1;
                            if ("UNAVAILABLE".equals(mode))
                                throw new AmlProblem(
                                        "MOCK-UNAVAILABLE", ErrorType.EXTERNAL_FAILURE);
                            String status =
                                    old == null
                                            ? switch (mode) {
                                                case "REJECT" -> "REJECTED";
                                                case "PENDING" -> "PENDING";
                                                default -> "ACCEPTED";
                                            }
                                            : old.status();
                            var next =
                                    new State(
                                            d.reportId(),
                                            d.reportKey(),
                                            d.hash(),
                                            status,
                                            deliveries,
                                            "ACCEPTED".equals(status) ? 1 : 0,
                                            old == null
                                                    ? java.time.Instant.now()
                                                    : old.createdAt());
                            write(d.reportKey() + ".json", next);
                            return next;
                        });
        if ("RESPONSE_LOSS".equals(mode(d.reportKey())))
            try {
                Thread.sleep(4200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        return result(state);
    }

    @PostMapping("/mock/v1/kofiu/report-queries")
    public KofiuResult query(
            @RequestHeader("X-Kofiu-Token") String credential, @RequestBody Query q) {
        authorize(credential, token);
        validate(q);
        return locked(
                q.reportKey(),
                () -> {
                    if (Files.exists(
                            directory.resolve(
                                    com.shinhan.pg.common.amlcontracts.AmlHashes.hash(q.reportKey())
                                            + ".expired"))) throw AmlProblem.conflict();
                    State old = read(q.reportKey());
                    if (old == null)
                        return new KofiuResult(
                                q.reportId(),
                                q.reportKey(),
                                q.contentHash(),
                                null,
                                "SENT",
                                "NOT_RECEIVED",
                                0,
                                0);
                    if (!old.hash().equals(q.contentHash()) || !old.reportId().equals(q.reportId()))
                        throw AmlProblem.conflict();
                    return result(old);
                });
    }

    @PostMapping("/mock/v1/kofiu/scenarios")
    public void scenario(
            @RequestHeader("X-Kofiu-Operations-Token") String credential, @RequestBody Scenario s) {
        authorize(credential, operations);
        key(s.reportKey());
        AmlProblem.require(
                Set.of("SUCCESS", "RESPONSE_LOSS", "UNAVAILABLE", "PENDING", "REJECT")
                        .contains(s.mode()));
        locked(
                s.reportKey(),
                () -> {
                    write(s.reportKey() + ".mode", s);
                    return true;
                });
    }

    @PostMapping("/mock/v1/kofiu/results")
    public KofiuResult finalizeResult(
            @RequestHeader("X-Kofiu-Operations-Token") String credential,
            @RequestBody FinalResult f) {
        authorize(credential, operations);
        key(f.reportKey());
        AmlProblem.require(Set.of("ACCEPTED", "REJECTED").contains(f.status()));
        return locked(
                f.reportKey(),
                () -> {
                    State old = read(f.reportKey());
                    if (old == null) throw AmlProblem.missing();
                    if (!"PENDING".equals(old.status()) && !old.status().equals(f.status()))
                        throw AmlProblem.conflict();
                    State next =
                            new State(
                                    old.reportId(),
                                    old.key(),
                                    old.hash(),
                                    f.status(),
                                    old.deliveries(),
                                    "ACCEPTED".equals(f.status()) ? 1 : 0,
                                    old.createdAt());
                    write(f.reportKey() + ".json", next);
                    return result(next);
                });
    }

    private KofiuResult result(State s) {
        return new KofiuResult(
                s.reportId(),
                s.key(),
                s.hash(),
                "MOCK_" + s.reportId(),
                "SENT",
                s.status(),
                s.deliveries(),
                s.acceptances());
    }

    private String mode(String key) {
        try {
            Path p = directory.resolve(key + ".mode");
            return Files.exists(p)
                    ? json.readValue(Files.readAllBytes(p), Scenario.class).mode()
                    : "SUCCESS";
        } catch (Exception ex) {
            throw new IllegalStateException("Mock storage unavailable");
        }
    }

    private State read(String key) {
        try {
            Path p = directory.resolve(key + ".json");
            if (!Files.exists(p)) return null;
            State state = json.readValue(Files.readAllBytes(p), State.class);
            if (state.createdAt() == null
                    || state.createdAt().isBefore(java.time.Instant.now().minusSeconds(172800))) {
                expire(key, state);
                throw AmlProblem.conflict();
            }
            return state;
        } catch (AmlProblem ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Mock storage unavailable");
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
    public void purgeExpired() {
        synchronized (mutex) {
            try (var files = Files.list(directory)) {
                for (Path p :
                        files.filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                    var state = json.readValue(Files.readAllBytes(p), State.class);
                    if (state.createdAt() == null
                            || state.createdAt()
                                    .isBefore(java.time.Instant.now().minusSeconds(172800)))
                        locked(
                                state.key(),
                                () -> {
                                    expire(state.key(), state);
                                    return true;
                                });
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Mock storage unavailable");
            }
        }
    }

    private void expire(String key, State state) {
        write(
                com.shinhan.pg.common.amlcontracts.AmlHashes.hash(key) + ".expired",
                java.util.Map.of("contentHash", state.hash()));
        try {
            Files.deleteIfExists(directory.resolve(key + ".json"));
            Files.deleteIfExists(directory.resolve(key + ".mode"));
        } catch (Exception ex) {
            throw new IllegalStateException("Mock storage unavailable");
        }
    }

    private void write(String name, Object value) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile(directory, "pending-", ".tmp");
            byte[] data = json.writeValueAsBytes(value);
            try (var channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(data);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(
                    tmp,
                    directory.resolve(name),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            try (var channel = FileChannel.open(directory, StandardOpenOption.READ)) {
                channel.force(true);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Mock storage unavailable");
        } finally {
            if (tmp != null)
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception ignored) {
                }
        }
    }

    private <T> T locked(String key, java.util.function.Supplier<T> work) {
        key(key);
        synchronized (mutex) {
            try (var channel =
                            FileChannel.open(
                                    directory.resolve(
                                            com.shinhan.pg.common.amlcontracts.AmlHashes.hash(key)
                                                    + ".lock"),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE);
                    var lock = channel.lock()) {
                return work.get();
            } catch (AmlProblem ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IllegalStateException("Mock storage unavailable");
            }
        }
    }

    private static void authorize(String supplied, String expected) {
        if (expected.isBlank()
                || supplied == null
                || !MessageDigest.isEqual(
                        supplied.getBytes(StandardCharsets.UTF_8),
                        expected.getBytes(StandardCharsets.UTF_8)))
            throw new AmlProblem("MOCK-FORBIDDEN", ErrorType.FORBIDDEN);
    }

    private static void key(String key) {
        AmlProblem.require(key != null && key.matches("[A-Za-z0-9_-]{1,128}"));
    }

    private static void validate(Query q) {
        key(q.reportKey());
        AmlProblem.require(
                q.reportId() != null
                        && q.contentHash() != null
                        && q.contentHash().matches("[a-f0-9]{64}"));
    }

    public record State(
            UUID reportId,
            String key,
            String hash,
            String status,
            int deliveries,
            int acceptances,
            java.time.Instant createdAt) {}

    public record Query(UUID reportId, String reportKey, String contentHash) {}

    public record Scenario(String reportKey, String mode) {}

    public record FinalResult(String reportKey, String status) {}
}
