package th.ac.kku.freelance_hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FreelanceHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(FreelanceHubApplication.class, args);
	}

}
