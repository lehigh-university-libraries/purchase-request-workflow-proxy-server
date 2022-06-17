package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.email;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import edu.lehigh.libraries.purchase_request.model.PurchaseRequest;
import edu.lehigh.libraries.purchase_request.model.PurchasedItem;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.config.Config;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.WorkflowServiceListener;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.post_purchase.PostPurchaseService;
import edu.lehigh.libraries.purchase_request.workflow_proxy_server.storage.WorkflowService;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnWebApplication
@Slf4j
public class EmailListener implements WorkflowServiceListener {

    private static final String FORMAT_ELECTRONIC_FILENAME_SUFFIX = "-electronic";

    private final String SUBJECT_PREFIX;
    private final String FROM_ADDRESS;
    private final String PURCHASE_REQUESTED_ADDRESS;
    private final String PURCHASE_APPROVED_ADDRESS;
    private final String PURCHASE_DENIED_ADDRESS;
    private final String PURCHASE_ARRIVED_ADDRESS;
    private final String ADDRESS_DOMAIN;
    private final Duration PURCHASE_REQUESTED_DELAY;
    private final Duration PURCHASE_APPROVED_DELAY;
    private final Duration PURCHASE_DENIED_DELAY;
    private final Duration PURCHASE_ARRIVED_DELAY;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private TemplateEngine emailTemplateEngine;

    @Autowired
    private PostPurchaseService postPurchaseService;

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
        PURCHASE_REQUESTED_DELAY = config.getEmail().getPurchaseRequestedDelay();
        PURCHASE_APPROVED_DELAY = config.getEmail().getPurchaseApprovedDelay();
        PURCHASE_DENIED_DELAY = config.getEmail().getPurchaseDeniedDelay();
        PURCHASE_ARRIVED_DELAY = config.getEmail().getPurchaseArrivedDelay();

        log.debug("EmailListener listening.");
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase requested.");
        email(new PurchaseRequestedEmailer(purchaseRequest.getKey()), PURCHASE_REQUESTED_DELAY);
    }

    private void emailPurchaseRequested(PurchaseRequest purchaseRequest) {
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
        message.setText(buildText("requested", purchaseRequest));

        emailSender.send(message);
        log.info("Emailed new purchase request: " + message.getText());
}

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase approved.");
        email(new PurchaseApprovedEmailer(purchaseRequest.getKey()), PURCHASE_APPROVED_DELAY);
    }

    private void emailPurchaseApproved(PurchaseRequest purchaseRequest) {
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
        message.setText(buildText("approved", purchaseRequest));

        emailSender.send(message);
        log.info("Emailed approved purchase request: " + message.getText());
    }

    @Override
    public void purchaseDenied(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase denied.");
        email(new PurchaseDeniedEmailer(purchaseRequest.getKey()), PURCHASE_DENIED_DELAY);
    }

    private void emailPurchaseDenied(PurchaseRequest purchaseRequest) {
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
        message.setText(buildText("denied", purchaseRequest));

        emailSender.send(message);
        log.info("Emailed denied purchase request: " + message.getText());
    }

    @Override
    public void purchaseArrived(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase arrived.");
        email(new PurchaseArrivedEmailer(purchaseRequest.getKey()), PURCHASE_ARRIVED_DELAY);
    }

    private void emailPurchaseArrived(PurchaseRequest purchaseRequest) {
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
        PurchasedItem purchasedItem = postPurchaseService.getFromRequest(purchaseRequest);
        String templateName = determineTemplate("arrived", purchasedItem);
        message.setText(buildText(templateName, purchaseRequest, purchasedItem));

        emailSender.send(message);
        log.info("Emailed arrived purchase request: " + message.getText());
    }

    private String determineTemplate(String statusPart, PurchasedItem purchasedItem) {
        if (purchasedItem != null && purchasedItem.getElectronicAccessUrl() != null) {
            return statusPart + FORMAT_ELECTRONIC_FILENAME_SUFFIX;
        }
        return statusPart;
    }

    private void email(Emailer emailer, Duration delay) {
        if (delay == null) {
            emailer.run();
        }
        else {
            LocalDateTime sendTime = LocalDateTime.now().plus(delay);
            taskScheduler.schedule(emailer, sendTime.atZone(ZoneId.systemDefault()).toInstant());
        }
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

    private String buildText(String templateName, PurchaseRequest purchaseRequest) {
        return buildText(templateName, purchaseRequest, null);
    }

    private String buildText(String templateName, PurchaseRequest purchaseRequest, PurchasedItem purchasedItem) {
        log.debug("Using template " + templateName);
        Context context = new Context();
        context.setVariable("purchaseRequest", purchaseRequest);
        context.setVariable("workflowUrl", workflowService.getWebUrl(purchaseRequest));
        if (purchasedItem != null) {
            context.setVariable("purchasedItem", purchasedItem);
        }
        return emailTemplateEngine.process(templateName, context);
    }

    private abstract class Emailer implements Runnable{
        String key;
        Emailer(String key) { this.key = key;}
    }
    
    private class PurchaseRequestedEmailer extends Emailer {
        PurchaseRequestedEmailer(String key) { super(key); }

        @Override
        public void run() {
            PurchaseRequest purchaseRequest = workflowService.findByKey(key);
            emailPurchaseRequested(purchaseRequest);            
        }
    }

    private class PurchaseApprovedEmailer extends Emailer {
        PurchaseApprovedEmailer(String key) { super(key); }

        @Override
        public void run() {
            PurchaseRequest purchaseRequest = workflowService.findByKey(key);
            emailPurchaseApproved(purchaseRequest);            
        }
    }

    private class PurchaseDeniedEmailer extends Emailer {
        PurchaseDeniedEmailer(String key) { super(key); }

        @Override
        public void run() {
            PurchaseRequest purchaseRequest = workflowService.findByKey(key);
            emailPurchaseDenied(purchaseRequest);            
        }
    }

    private class PurchaseArrivedEmailer extends Emailer {
        PurchaseArrivedEmailer(String key) { super(key); }

        @Override
        public void run() {
            PurchaseRequest purchaseRequest = workflowService.findByKey(key);
            emailPurchaseArrived(purchaseRequest);            
        }
    }

}
