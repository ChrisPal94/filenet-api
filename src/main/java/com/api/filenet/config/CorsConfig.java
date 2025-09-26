package com.api.filenet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry
      .addMapping("/**") // aplica a todos los endpoints
      .allowedOrigins("*") // permite todos los orígenes
      .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
      .allowedHeaders("*");
    // .allowCredentials(true); ⚠️ NO usar con "*" (rompe CORS)
  }
}
