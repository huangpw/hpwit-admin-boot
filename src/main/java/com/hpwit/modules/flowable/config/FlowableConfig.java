package com.hpwit.modules.flowable.config;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "hpwit.flowable", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ApprovalCallbackProperties.class)
@EnableScheduling
public class FlowableConfig {

    @Bean(destroyMethod = "close")
    public ProcessEngine approvalProcessEngine(
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            @Value("${hpwit.flowable.schema-update:false}") boolean schemaUpdate) {
        var configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setDatabaseSchemaUpdate(Boolean.toString(schemaUpdate));
        configuration.setHistoryLevel(HistoryLevel.AUDIT);
        configuration.setDisableIdmEngine(true);
        configuration.setDisableEventRegistry(true);
        configuration.setAsyncExecutorActivate(false);
        configuration.setBeans(Map.of());
        return configuration.buildProcessEngine();
    }
}
