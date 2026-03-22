package com.finadvise.crm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.oracle.OracleContainer;

@SpringBootTest
@ActiveProfiles("test")
class CrmApplicationTests  {

	@Container
	@ServiceConnection
	static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart");
	@Test
	void contextLoads() {
	}

}
