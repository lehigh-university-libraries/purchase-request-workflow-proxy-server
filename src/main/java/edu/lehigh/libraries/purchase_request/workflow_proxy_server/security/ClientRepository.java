package edu.lehigh.libraries.purchase_request.workflow_proxy_server.security;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Client findByClientName(String clientName);
    
}
