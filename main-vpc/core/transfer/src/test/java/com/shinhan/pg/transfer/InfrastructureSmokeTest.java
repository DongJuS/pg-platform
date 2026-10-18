package com.shinhan.pg.transfer;

import static org.junit.jupiter.api.Assertions.*;

import com.shinhan.pg.common.archrules.InfrastructureTestBase;
import com.shinhan.pg.transfer.application.port.in.RequestTransferUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@EnabledIfSystemProperty(named = "integration", matches = "true")
@SpringBootTest
class InfrastructureSmokeTest extends InfrastructureTestBase {
    @Autowired HealthEndpoint health;
    @Autowired ApplicationContext context;

    @Test
    void postgresKafkaRedisAreHealthy() {
        assertTrue(context.getBeansOfType(RequestTransferUseCase.class).isEmpty());
        assertEquals(Status.UP, health.health().getStatus());
        for (var component : java.util.List.of("db", "redis", "kafka")) {
            assertNotNull(health.healthForPath(component), component);
            assertEquals(Status.UP, health.healthForPath(component).getStatus(), component);
        }
    }
}
