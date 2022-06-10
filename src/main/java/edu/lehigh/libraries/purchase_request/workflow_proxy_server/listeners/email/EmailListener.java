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
    private final String PURCHASE_REQUESTED_ADDRESS;
    private final String PURCHASE_APPROVED_ADDRESS;
    private final String PURCHASE_DENIED_ADDRESS;
    private final String PURCHASE_ARRIVED_ADDRESS;
    private final String ADDRESS_DOMAIN;

    @Autowired
    private JavaMailSender emailSender;

    private WorkflowService workflowService;

    EmailListener(WorkflowService service, Config config) {
        service.addListener(this);
        this.workflowService = service;

        SUBJECT_PREFIX = config.getEmail().getSubjectPrefix();
        FROM_ADDRESS = config.getEmail().getFromAddress();
        PURCHASE_REQUESTED_ADDRESS = config.getEmail().getPurchaseRequestedAddress();
        PURCHASE_APPROVED_ADDRESS = config.getEmail().getPurchaseApprovedAddress();
        PURCHASE_DENIED_ADDRESS = config.getEmail().getPurchaseDeniedAddress();
        PURCHASE_ARRIVED_ADDRESS = config.getEmail().getPurchaseArrivedAddress();
        ADDRESS_DOMAIN = config.getEmail().getAddressDomain();

        log.debug("EmailListener listening.");
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase requested.");
        SimpleMailMessage message = buildStubMessage();

        // Subject
        message.setSubject(buildSubject("New Purchase Requested at " + purchaseRequest.getCreationDate()));

        // Recipients
        List<String> recipients = new ArrayList<String>();
        addLibrarianRecipient(recipients, purchaseRequest);
        if (PURCHASE_REQUESTED_ADDRESS != null) {
            recipients.add(PURCHASE_REQUESTED_ADDRESS);
        }
        if (recipients.size() == 0) {
            return;
        }
        message.setTo(recipients.toArray(new String[0]));

        // Body
        String text = "Purchase Request received via " + purchaseRequest.getClientName()
            + "\n\n"
            + "Title: " + purchaseRequest.getTitle() + "\n"
            + "Contributor: " + purchaseRequest.getContributor() + "\n"
            + "Workflow URL: " + workflowService.getWebUrl(purchaseRequest) + "\n"
            + "";
        message.setText(text);

        emailSender.send(message);
        log.debug("Emailed new purchase request.");
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase approved.");
        SimpleMailMessage message = buildStubMessage();

        // Subject
        message.setSubject(buildSubject("Purchase Approved at " + purchaseRequest.getCreationDate()));

        // Recipients
        List<String> recipients = new ArrayList<String>();
        if (PURCHASE_APPROVED_ADDRESS != null) {
            recipients.add(PURCHASE_APPROVED_ADDRESS);
        }
        if (recipients.size() == 0) {
            return;
        }
        message.setTo(recipients.toArray(new String[0]));

        // Body
        String text = "Purchase Request approved"
            + "\n\n"
            + "Title: " + purchaseRequest.getTitle() + "\n"
            + "Contributor: " + purchaseRequest.getContributor() + "\n"
            + "Workflow URL: " + workflowService.getWebUrl(purchaseRequest) + "\n"
            + "";
        message.setText(text);

        emailSender.send(message);
        log.debug("Emailed approved purchase request.");
    }

    @Override
    public void purchaseDenied(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase denied.");
        SimpleMailMessage message = buildStubMessage();

        // Subject
        message.setSubject(buildSubject("Purchase Denied at " + purchaseRequest.getCreationDate()));

        // Recipients
        List<String> recipients = new ArrayList<String>();
        if (PURCHASE_DENIED_ADDRESS != null) {
            recipients.add(PURCHASE_DENIED_ADDRESS);
        }
        if (recipients.size() == 0) {
            return;
        }
        message.setTo(recipients.toArray(new String[0]));

        // Body
        String text = "Purchase Request denied"
            + "\n\n"
            + "Title: " + purchaseRequest.getTitle() + "\n"
            + "Contributor: " + purchaseRequest.getContributor() + "\n"
            + "Requester comments: " + purchaseRequest.getRequesterComments() + "\n"
            + "Workflow URL: " + workflowService.getWebUrl(purchaseRequest) + "\n"
            + "";
        message.setText(text);

        emailSender.send(message);
        log.debug("Emailed denied purchase request.");
    }

    @Override
    public void purchaseArrived(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase arrived.");
        SimpleMailMessage message = buildStubMessage();

        // Subject
        message.setSubject(buildSubject("Purchase Arrived"));

        // Recipients
        List<String> recipients = new ArrayList<String>();
        addRequesterRecipient(recipients, purchaseRequest);
        if (PURCHASE_ARRIVED_ADDRESS != null) {
            recipients.add(PURCHASE_ARRIVED_ADDRESS);
        }
        if (recipients.size() == 0) {
            return;
        }
        message.setTo(recipients.toArray(new String[0]));

        // Body
        String text = "Purchase Request arrived"
            + "\n\n"
            + "Title: " + purchaseRequest.getTitle() + "\n"
            + "Contributor: " + purchaseRequest.getContributor() + "\n"
            + "";
        message.setText(text);

        emailSender.send(message);
        log.debug("Emailed arrived purchase request.");
    }

    private SimpleMailMessage buildStubMessage() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        return message;
    }

    private String buildSubject(String subject) {
        if (SUBJECT_PREFIX != null) {
            subject = SUBJECT_PREFIX + subject;
        }
        return subject;
    }

    private void addLibrarianRecipient(List<String> recipients, PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getLibrarianUsername() != null) {
            recipients.add(purchaseRequest.getLibrarianUsername() + '@' + ADDRESS_DOMAIN);
        }
    }

    private void addRequesterRecipient(List<String> recipients, PurchaseRequest purchaseRequest) {
        if (purchaseRequest.getRequesterUsername() != null) {
            recipients.add(purchaseRequest.getRequesterUsername() + '@' + ADDRESS_DOMAIN);
        }
    }

}
