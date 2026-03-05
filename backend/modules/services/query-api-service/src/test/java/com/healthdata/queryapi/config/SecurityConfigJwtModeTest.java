package com.healthdata.queryapi.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies SecurityConfig supports both JWKS (RSA) and HMAC JWT modes (B6).
 */
@Tag("unit")
class SecurityConfigJwtModeTest {

    @Test
    void securityConfig_shouldHaveJwtModeField() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("jwtMode");
        assertThat(field).isNotNull();

        Value valueAnnotation = field.getAnnotation(Value.class);
        assertThat(valueAnnotation).isNotNull();
        assertThat(valueAnnotation.value()).contains("jwks");
    }

    @Test
    void securityConfig_shouldHaveJwkSetUriField() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("jwkSetUri");
        assertThat(field).isNotNull();

        Value valueAnnotation = field.getAnnotation(Value.class);
        assertThat(valueAnnotation).isNotNull();
        assertThat(valueAnnotation.value()).contains("jwk-set-uri");
    }

    @Test
    void securityConfig_shouldHaveJwtDecoderMethod() throws Exception {
        Method method = SecurityConfig.class.getDeclaredMethod("jwtDecoder");
        assertThat(method).isNotNull();
        assertThat(method.getReturnType().getSimpleName()).isEqualTo("JwtDecoder");
    }
}
