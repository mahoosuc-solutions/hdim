package com.healthdata.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.healthdata.audit.service.AuditService;
import com.healthdata.audit.repository.shared.AuditEventRepository;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.repository.ai.ConfigurationEngineEventRepository;
import com.healthdata.audit.repository.ai.UserConfigurationActionEventRepository;
import com.healthdata.audit.repository.QAReviewRepository;
import com.healthdata.audit.repository.clinical.ClinicalDecisionRepository;
import com.healthdata.audit.repository.MPIMergeRepository;
import com.healthdata.audit.repository.DataQualityIssueRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "audit.enabled=false",
    "healthdata.persistence.jpa.auditing-enabled=false",
    "test.kafka.topics.enabled=false",
    "spring.autoconfigure.exclude="
        + "com.healthdata.database.config.DatabaseAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
@ActiveProfiles("test")
@Import(KafkaListenerDisablementTest.TestRedisConfig.class)
class KafkaListenerDisablementTest {

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @MockBean
    private AuditService auditService;

    @MockBean
    private AuditEventRepository auditEventRepository;

    @MockBean
    private AIAgentDecisionEventRepository aiAgentDecisionEventRepository;

    @MockBean
    private ConfigurationEngineEventRepository configurationEngineEventRepository;

    @MockBean
    private UserConfigurationActionEventRepository userConfigurationActionEventRepository;

    @MockBean
    private QAReviewRepository qaReviewRepository;

    @MockBean
    private ClinicalDecisionRepository clinicalDecisionRepository;

    @MockBean
    private MPIMergeRepository mpiMergeRepository;

    @MockBean
    private DataQualityIssueRepository dataQualityIssueRepository;

    @Test
    void kafkaListenersShouldNotAutoStartInTests() {
        registry.getListenerContainers()
            .forEach(container -> assertThat(container.isRunning()).isFalse());
    }

    @TestConfiguration
    static class TestRedisConfig {
        @Bean
        @Primary
        ReactiveRedisTemplate<String, String> reactiveRedisTemplate() {
            return org.mockito.Mockito.mock(ReactiveRedisTemplate.class);
        }

    }
}
