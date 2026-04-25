package tn.esprit.spring.baladna.accommodation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AccommodationStaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.accommodation.upload-dir:uploads/accommodations}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir;
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/accommodations/**")
                .addResourceLocations("file:" + location);
    }
}
