package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.email;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.WorkflowServiceListener;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnWebApplication
@Slf4j
public class EmailListener implements WorkflowServiceListener {

    private final String SUBJECT_PREFIX;
    private final String FROM_ADDRESS;
    private final String PURCHASE_REQUESTED_ADDRESSES;
    private final String ADDRESS_DOMAIN;

    @Autowired
    private JavaMailSender emailSender;

    EmailListener(WorkflowService service, Config config) {
        service.addListener(this);

        SUBJECT_PREFIX = config.getEmail().getSubjectPrefix();
        FROM_ADDRESS = config.getEmail().getFromAddress();
        PURCHASE_REQUESTED_ADDRESSES = config.getEmail().getPurchaseRequestedAddresses();
        ADDRESS_DOMAIN = config.getEmail().getAddressDomain();

        log.debug("EmailListener listening.");
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        SimpleMailMessage message = buildStubMessage();

        // Subject
        String subject = "New Purchase Requested at " + purchaseRequest.getCreationDate();
        if (SUBJECT_PREFIX != null) {
            subject = SUBJECT_PREFIX + subject;
        }
        message.setSubject(subject);

        // Recipients
        List<String> recipients = new ArrayList<String>();
        addLibrarianRecipients(recipients, purchaseRequest);
        if (PURCHASE_REQUESTED_ADDRESSES != null) {
            recipients.add(PURCHASE_REQUESTED_ADDRESSES);
        }
        message.setTo(recipients.toArray(new String[0]));

        // Body
        String text = "Purchase Request received via " + purchaseRequest.getClientName()
            + "\n\n"
            + "Title: " + purchaseRequest.getTitle() + "\n"
            + "Contributor: " + purchaseRequest.getContributor() + "\n"
            + "";
        message.setText(text);

        emailSender.send(message);
        log.debug("Emailed new purchase request.");
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        // TODO Actually send email
    }

    private SimpleMailMessage buildStubMessage() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        return message;
    }

    private void addLibrarianRecipients(List<String> recipients, PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getLibrarianUsername() != null) {
            recipients.add(purchaseRequest.getLibrarianUsername() + '@' + ADDRESS_DOMAIN);
        }
    }

}
