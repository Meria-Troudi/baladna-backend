package tn.esprit.spring.baladna.event.forum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
