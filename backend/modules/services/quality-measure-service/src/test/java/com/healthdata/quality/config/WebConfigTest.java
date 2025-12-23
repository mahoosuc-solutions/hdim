package com.healthdata.quality.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

@DisplayName("WebConfig")
class WebConfigTest {

    @Test
    @DisplayName("Should configure Tomcat to decode encoded slashes")
    void shouldConfigureEncodedSlashes() {
        WebConfig config = new WebConfig();
        var customizer = config.tomcatCustomizer();

        CaptureTomcatFactory factory = new CaptureTomcatFactory();
        customizer.customize(factory);

        assertThat(factory.customizers).hasSize(1);
        Connector connector = new Connector();
        factory.customizers.get(0).customize(connector);

        assertThat(connector.getEncodedSolidusHandling())
                .isEqualTo(EncodedSolidusHandling.DECODE.getValue());
    }

    private static final class CaptureTomcatFactory extends TomcatServletWebServerFactory {
        private final List<TomcatConnectorCustomizer> customizers = new ArrayList<>();

        @Override
        public void addConnectorCustomizers(TomcatConnectorCustomizer... customizers) {
            for (TomcatConnectorCustomizer customizer : customizers) {
                this.customizers.add(customizer);
            }
        }
    }
}
