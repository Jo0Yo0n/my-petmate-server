package io.github.jo0yo0n.mypetmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyPetmateServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(MyPetmateServerApplication.class, args);
  }
}
