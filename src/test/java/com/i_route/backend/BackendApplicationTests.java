package com.i_route.backend;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = BackendApplication.class)
class BackendApplicationTests {

	@MockitoBean
	private FirebaseApp firebaseApp;

	@Test
	void contextLoads() {
	}
}
