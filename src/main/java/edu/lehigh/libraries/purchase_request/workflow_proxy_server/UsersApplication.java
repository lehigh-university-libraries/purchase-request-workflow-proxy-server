package edu.lehigh.libraries.purchase_request.workflow_proxy_server;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import edu.lehigh.libraries.purchase_request.workflow_proxy_server.security.SecurityConfig;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.security.User;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.security.UserRepository;

@SpringBootApplication
@ConditionalOnNotWebApplication
public class UsersApplication implements CommandLineRunner {
    
    private static Logger log = LoggerFactory.getLogger(UsersApplication.class);

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

}
