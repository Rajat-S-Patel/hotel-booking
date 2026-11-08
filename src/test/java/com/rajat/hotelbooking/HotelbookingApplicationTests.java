package com.rajat.hotelbooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "hotelbooking.scheduling.enabled=false")
class HotelbookingApplicationTests {
	@Test
	void contextLoads() {
	}
}
