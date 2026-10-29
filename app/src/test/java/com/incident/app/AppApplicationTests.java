package com.incident.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/test-app.db")
class AppApplicationTests {

	@Test
	void contextLoads() {
	}

}
