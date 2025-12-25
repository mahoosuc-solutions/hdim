package com.healthdata.qrda;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.service.JwtTokenService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * QRDA Export Service Application
 *
 * Generates QRDA Category I (patient-level) and Category III (aggregate)
 * documents for CMS quality reporting submission.
 *
 * CMS Requirement: Web Interface sunset 2025 - ACOs must submit eCQMs via QRDA
 *
 * @see <a href="https://ecqi.healthit.gov/qrda">eCQI QRDA Standards</a>
 */
@SpringBootApplication
@Import({JwtAuthenticationFilter.class, JwtTokenService.class, JwtConfig.class})
@EnableFeignClients
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableJpaRepositories(basePackages = {
    "com.healthdata.qrda.persistence",
    "com.healthdata.authentication.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.qrda.persistence",
    "com.healthdata.authentication.domain",
    "com.healthdata.authentication.entity"
})
public class QrdaExportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QrdaExportServiceApplication.class, args);
    }
}
