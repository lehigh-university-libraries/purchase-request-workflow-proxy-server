package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ConditionalOnWebApplication
@Slf4j
public class WorkflowApplication {

	public static void main(String[] args) {
		log.info("Starting the Workflow Application");
		SpringApplication.run(WorkflowApplication.class, args);
        log.info("Workflow Application finished");
	}

}
