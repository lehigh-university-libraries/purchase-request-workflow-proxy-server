package edu.lehigh.libraries.purchase_request.workflow_proxy_server.security;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);
    
}
