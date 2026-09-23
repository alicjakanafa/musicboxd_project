package com.example.MusicBoxd;

import com.example.MusicBoxd.support.TestOAuth2Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("apptest")
@Import(TestOAuth2Config.class)
class MusicBoxdApplicationTests {

	@Test
	void contextLoads() {
	}

}
