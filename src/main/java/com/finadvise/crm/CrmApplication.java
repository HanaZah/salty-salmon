package com.finadvise.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
public class CrmApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(CrmApplication.class, args);
	}

	private static void loadDotEnv() {
		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(".env")) {
			props.load(fis);
			// This pushes every KEY=VALUE from .env into the System environment
			props.forEach((key, value) -> System.setProperty(key.toString(), value.toString()));
			System.out.println(".env variables loaded into System properties.");
		} catch (IOException e) {
			System.err.println("Warning: .env file not found. Falling back to system environment.");
		}
	}

}
