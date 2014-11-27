package org.snet.tresor.pdp.contexthandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
@EnableAutoConfiguration
public class PDPContextHandler {

	public static void main(String[] args) {
		SpringApplication.run(PDPContextHandler.class, args);
	}

}
