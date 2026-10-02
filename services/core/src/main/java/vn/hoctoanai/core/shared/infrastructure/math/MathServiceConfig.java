package vn.hoctoanai.core.shared.infrastructure.math;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MathServiceProperties.class)
class MathServiceConfig {

    @Bean
    MathServiceClient mathServiceClient(RestClient.Builder builder, MathServiceProperties properties) {
        return new MathServiceClient(builder, properties.baseUrl(), properties.connectTimeout(), MathJob::timeout);
    }
}
