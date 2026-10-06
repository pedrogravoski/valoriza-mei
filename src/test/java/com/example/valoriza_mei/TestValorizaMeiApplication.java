package com.example.valoriza_mei;

import org.springframework.boot.SpringApplication;

public class TestValorizaMeiApplication {

	public static void main(String[] args) {
		SpringApplication.from(ValorizaMeiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
