package com.shinhan.pg.account;

import com.shinhan.pg.account.application.port.in.OpenAccountUseCase;
import com.shinhan.pg.account.config.AccountSkeletonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AccountStartupTest {
    @Test void startsWithTheDefaultClassBasedTransactionProxy() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("skeleton");
            context.register(AccountSkeletonConfiguration.class, Transactions.class);
            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(OpenAccountUseCase.class));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions {
        @Bean DataSource dataSource() { return mock(DataSource.class); }
        @Bean DataSourceTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
    }
}
