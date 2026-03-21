plugins {
	java
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.finadvise"
version = "0.0.1-SNAPSHOT"
description = "crm"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2025.1.0"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.hashids:hashids:1.0.3")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.oracle.database.jdbc:ojdbc11")
	annotationProcessor("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:oracle-free:1.19.7")
	testImplementation("org.testcontainers:junit-jupiter:1.19.7")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
}

tasks.withType<Test> {
	useJUnitPlatform()

	val mockitoAgent = configurations.testRuntimeClasspath.get()
		.find { it.name.contains("mockito-core") }

	if (mockitoAgent != null) {
		jvmArgs("-javaagent:$mockitoAgent", "-Xshare:off")
	}

}
