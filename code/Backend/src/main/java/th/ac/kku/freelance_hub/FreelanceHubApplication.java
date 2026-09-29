package th.ac.kku.freelance_hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import th.ac.kku.freelance_hub.seed.LocalSeedCommand;

@SpringBootApplication
@EnableScheduling
public class FreelanceHubApplication {

	public static void main(String[] args) {
		if ("true".equalsIgnoreCase(System.getenv("LOCAL_SEED_RUN"))) {
			LocalSeedCommand.main(args);
			return;
		}
		SpringApplication.run(FreelanceHubApplication.class, args);
	}

}
