package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.web.context.WebApplicationContext;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.enrichment.EnrichmentManager;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.WorkflowServiceListener;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ComponentScan(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@ConditionalOnWebApplication
@Slf4j
public class WorkflowApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(WorkflowApplication.class);
	}

	@Override
	protected WebApplicationContext run(SpringApplication application) {
		WebApplicationContext context = super.run(application);
		reportBuild(context);
		reportServices(context);
		return context;
	}

	private static void reportBuild(ApplicationContext context) {
		BuildProperties buildProperties = context.getBean(BuildProperties.class);
		log.info("Build time: " + buildProperties.getTime());
	}

	private static void reportServices(ApplicationContext context) {
		String[] beanNames = context.getBeanDefinitionNames();
		reportServices(beanNames, EnrichmentManager.class);
		reportServices(beanNames, WorkflowServiceListener.class);
	}

	private static void reportServices(String[] beanNames, Class<?> clazz) {
		List<String> services = Stream.of(beanNames)
			.filter(name -> name.contains(clazz.getPackageName()))
			.filter(name -> !name.equals(clazz.getCanonicalName()))
			.map(name -> name.substring(clazz.getPackageName().length() + 1))
			.collect(Collectors.toList());
		log.info(services.size() + " " + clazz.getSimpleName() + " services loaded:");
		for (String name: services) {
			log.info("... " + name);
		}
	}

	public static void main(String[] args) {
		log.info("Starting the Workflow Application");
		ApplicationContext context = SpringApplication.run(WorkflowApplication.class, args);
		reportBuild(context);
		reportServices(context);
        log.info("Workflow Application started");
	}

}
