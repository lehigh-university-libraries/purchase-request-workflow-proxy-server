package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

@SpringBootApplication
@ConditionalOnWebApplication
public class WorkflowApplication {

	private static Logger log = LoggerFactory.getLogger(WorkflowApplication.class);

	public static void main(String[] args) {
		log.info("Starting the Workflow Application");
		SpringApplication.run(WorkflowApplication.class, args);
        log.info("Workflow Application finished");
	}

}
