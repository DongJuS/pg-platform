package com.shinhan.pg.common.archrules;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
@Testcontainers
public abstract class InfrastructureTestBase {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:17-alpine");
    @Container static final KafkaContainer KAFKA=new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.0.1"));
    @Container static final GenericContainer<?> REDIS=new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    @DynamicPropertySource static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",POSTGRES::getUsername);
        registry.add("spring.datasource.password",POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers",KAFKA::getBootstrapServers);
        registry.add("spring.data.redis.host",REDIS::getHost);
        registry.add("spring.data.redis.port",() -> REDIS.getMappedPort(6379));
        registry.add("management.otlp.tracing.export.enabled",() -> false);
    }
}
