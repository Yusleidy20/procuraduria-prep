package com.yusdev.procuraduriaprep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ProcuraduriaPrepApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcuraduriaPrepApplication.class, args);
	}

}
