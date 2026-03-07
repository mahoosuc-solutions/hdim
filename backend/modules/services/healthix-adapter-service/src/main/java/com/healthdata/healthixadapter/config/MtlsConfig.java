package com.healthdata.healthixadapter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * mTLS configuration for Healthix adapter.
 * Required for FULL PHI classification — Healthix is a covered entity
 * and all communication must be mutually authenticated.
 */
@Configuration
@ConditionalOnProperty(name = "external.healthix.mtls.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MtlsConfig {

    private final HealthixProperties properties;

    @Bean
    public SSLContext healthixSslContext() throws Exception {
        HealthixProperties.Mtls mtls = properties.getMtls();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream kis = new FileInputStream(mtls.getKeystorePath())) {
            keyStore.load(kis, mtls.getKeystorePassword().toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, mtls.getKeystorePassword().toCharArray());

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream tis = new FileInputStream(mtls.getTruststorePath())) {
            trustStore.load(tis, mtls.getTruststorePassword().toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        log.info("mTLS SSLContext initialized for Healthix communication");
        return sslContext;
    }
}
