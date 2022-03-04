package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.security.SecurityConfig;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.security.Client;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.security.ClientRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ConditionalOnNotWebApplication
@Slf4j
public class ClientsApplication implements CommandLineRunner {
    
    private static final String
        COMMAND_ADD_CLIENT = "addclient",
        COMMAND_DELETE_CLIENT = "deleteclient";

    public static PasswordEncoder passwordEncoder = SecurityConfig.passwordEncoder();

    @Autowired
    private ClientRepository clientRepository;


    @Override
    public void run(String... args) {
        String command = args[0];
        String clientName = args[1];
        if (Objects.equals(COMMAND_ADD_CLIENT, command)) {
            String password = args[2];
            addClient(clientName, password);
        }
        else if (Objects.equals(COMMAND_DELETE_CLIENT, command)) {
            deleteClient(clientName);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    private void addClient(String clientName, String password) {
        Client existingClient = clientRepository.findByClientName(clientName);
        if (existingClient != null) {
            throw new IllegalArgumentException("client name " + clientName + " already exists");
        }

        Client client = new Client();
        client.setClientName(clientName);
        String encodedPassword = passwordEncoder.encode(password);
        client.setPassword(encodedPassword);
        clientRepository.save(client);
        log.info("Client " + client.getClientName() + " added.");
    }

    private void deleteClient(String clientName) {
        Client client = clientRepository.findByClientName(clientName);
        if (client == null) {
            throw new IllegalArgumentException("client name " + clientName + " does not exist");
        }
        clientRepository.delete(client);
        log.info("Client " + client.getClientName() + " deleted.");
    }

    public static void main(String[] args) {
        log.info("Starting the Clients Application");
        SpringApplication application = new SpringApplication(ClientsApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
        log.info("Clients Application finished");
    }

    // Only for this controller
	@Bean(name = "mvcHandlerMappingIntrospector")
	public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
		return new HandlerMappingIntrospector();
	}

}
