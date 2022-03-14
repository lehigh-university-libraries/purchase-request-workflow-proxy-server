package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.WorkflowServiceListener;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailListener implements WorkflowServiceListener {

    private final String FROM_ADDRESS;
    private final String PURCHASE_REQUESTED_ADDRESSES;

    @Autowired
    private JavaMailSender emailSender;

    EmailListener(WorkflowService service, Config config) {
        service.addListener(this);

        FROM_ADDRESS = config.getEmail().getFromAddress();
        PURCHASE_REQUESTED_ADDRESSES = config.getEmail().getPurchaseRequestedAddresses();
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(PURCHASE_REQUESTED_ADDRESSES);
        message.setSubject("New Purchase Requested at " + purchaseRequest.getCreationDate());

        String text = "Purchase Request received via " + purchaseRequest.getClientName()
            + "\n\n"
            + "Title: " + purchaseRequest.getTitle() + "\n"
            + "Contributor: " + purchaseRequest.getContributor() + "\n"
            + "";
        message.setText(text);

        emailSender.send(message);
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        // TODO Actually send email
        log.debug("Pretending to send mail for approved request: " + purchaseRequest);
    }

}
