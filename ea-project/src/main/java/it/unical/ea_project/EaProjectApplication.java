package it.unical.ea_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration.class)
public class EaProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(EaProjectApplication.class, args);
	}

}
