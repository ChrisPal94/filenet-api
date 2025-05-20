package com.api.filenet;

import com.api.filenet.config.FilenetProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FilenetProperties.class)
public class FilenetApplication {

  public static void main(String[] args) {
    io.github.cdimascio.dotenv.Dotenv dotenv =
      io.github.cdimascio.dotenv.Dotenv.load();

    dotenv
      .entries()
      .forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    SpringApplication.run(FilenetApplication.class, args);
  }
}
