package me.thinkcat.opic.practice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpicPracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpicPracticeApplication.class, args);
	}

}
