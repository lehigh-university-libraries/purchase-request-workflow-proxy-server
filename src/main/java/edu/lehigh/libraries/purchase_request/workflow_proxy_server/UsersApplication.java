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
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.security.User;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.security.UserRepository;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ConditionalOnNotWebApplication
@Slf4j
public class UsersApplication implements CommandLineRunner {
    
    private static final String
        COMMAND_ADD_USER = "adduser",
        COMMAND_DELETE_USER = "deleteuser";

    public static PasswordEncoder passwordEncoder = SecurityConfig.passwordEncoder();

    @Autowired
    private UserRepository userRepository;


    @Override
    public void run(String... args) {
        String command = args[0];
        String user = args[1];
        if (Objects.equals(COMMAND_ADD_USER, command)) {
            String password = args[2];
            addUser(user, password);
        }
        else if (Objects.equals(COMMAND_DELETE_USER, command)) {
            deleteUser(user);
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    private void addUser(String username, String password) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser != null) {
            throw new IllegalArgumentException("username " + username + " already exists");
        }

        User user = new User();
        user.setUsername(username);
        String encodedPassword = passwordEncoder.encode(password);
        user.setPassword(encodedPassword);
        userRepository.save(user);
        log.info("User " + user.getUsername() + " added.");
    }

    private void deleteUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("username " + username + " does not exist");
        }
        userRepository.delete(user);
        log.info("User " + user.getUsername() + " deleted.");
    }

    public static void main(String[] args) {
        log.info("Starting the Users Application");
        SpringApplication application = new SpringApplication(UsersApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
        log.info("Users Application finished");
    }

    // Only for this controller
	@Bean(name = "mvcHandlerMappingIntrospector")
	public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
		return new HandlerMappingIntrospector();
	}

}
