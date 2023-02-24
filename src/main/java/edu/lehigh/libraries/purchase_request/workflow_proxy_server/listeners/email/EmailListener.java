package edu.lehigh.libraries.purchase_request.workflow_proxy_server.listeners.email;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final String 
        PURCHASE_REQUESTED_ADDRESS,
        PURCHASE_APPROVED_ADDRESS,
        PURCHASE_DENIED_ADDRESS,
        PURCHASE_ARRIVED_ADDRESS;
    private final String ADDRESS_DOMAIN;
    private final boolean
        PURCHASE_REQUESTED_HTML, 
        PURCHASE_APPROVED_HTML, 
        PURCHASE_DENIED_HTML, 
        PURCHASE_ARRIVED_HTML; 
    private final Duration 
        PURCHASE_REQUESTED_DELAY,
        PURCHASE_APPROVED_DELAY,
        PURCHASE_DENIED_DELAY,
        PURCHASE_ARRIVED_DELAY;
    private final Map<String, String> PURCHASE_DENIED_REASONS;

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

        PURCHASE_REQUESTED_HTML = config.getEmail().getPurchaseRequestedHtml();
        PURCHASE_APPROVED_HTML = config.getEmail().getPurchaseApprovedHtml();
        PURCHASE_DENIED_HTML = config.getEmail().getPurchaseDeniedHtml();
        PURCHASE_ARRIVED_HTML = config.getEmail().getPurchaseArrivedHtml();

        PURCHASE_REQUESTED_DELAY = config.getEmail().getPurchaseRequestedDelay();
        PURCHASE_APPROVED_DELAY = config.getEmail().getPurchaseApprovedDelay();
        PURCHASE_DENIED_DELAY = config.getEmail().getPurchaseDeniedDelay();
        PURCHASE_ARRIVED_DELAY = config.getEmail().getPurchaseArrivedDelay();
        PURCHASE_DENIED_REASONS = config.getEmail().getPurchaseDeniedReasons();

        log.debug("EmailListener listening.");
    }

    @Override
    public void purchaseRequested(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase requested.");
        email(new PurchaseRequestedEmailer(purchaseRequest.getKey()), PURCHASE_REQUESTED_DELAY);
    }

    private void emailPurchaseRequested(PurchaseRequest purchaseRequest) {
        try {            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            prepStubMessage(helper);

            // Subject
            message.setSubject(buildSubject("New Purchase Requested at " + purchaseRequest.getCreationDate()));

            // Recipients
            addLibrarianRecipient(message, purchaseRequest);
            if (PURCHASE_REQUESTED_ADDRESS != null) {
                addToRecipient(message, PURCHASE_REQUESTED_ADDRESS);
            }
            if (getRecipientCount(message) == 0) {
                return;
            }

            // Body
            String text = buildText("requested", purchaseRequest);
            helper.setText(text, PURCHASE_REQUESTED_HTML);

            emailSender.send(message);
            log.info("Emailed new purchase request: " + text);
        }
        catch (MessagingException e) {
            log.error("Failed to email new purchase request", e);
        }
    }

    @Override
    public void purchaseApproved(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase approved.");
        email(new PurchaseApprovedEmailer(purchaseRequest.getKey()), PURCHASE_APPROVED_DELAY);
    }

    private void emailPurchaseApproved(PurchaseRequest purchaseRequest) {
        try {            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            prepStubMessage(helper);

            // Subject
            message.setSubject(buildSubject("Purchase Approved at " + purchaseRequest.getCreationDate()));

            // Recipients
            if (PURCHASE_APPROVED_ADDRESS != null) {
                addToRecipient(message, PURCHASE_APPROVED_ADDRESS);
            }
            if (getRecipientCount(message) == 0) {
                return;
            }

            // Body
            String text = buildText("approved", purchaseRequest);
            helper.setText(text, PURCHASE_APPROVED_HTML);

            emailSender.send(message);
            log.info("Emailed approved purchase request: " + text);
        }
        catch (MessagingException e) {
            log.error("Failed to email approved purchase request", e);
        }
    }

    @Override
    public void purchaseDenied(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase denied.");
        email(new PurchaseDeniedEmailer(purchaseRequest.getKey()), PURCHASE_DENIED_DELAY);
    }

    private void emailPurchaseDenied(PurchaseRequest purchaseRequest) {
        try {            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            prepStubMessage(helper);

            // Subject
            message.setSubject(buildSubject("Purchase Denied at " + purchaseRequest.getCreationDate()));
                
            // Recipients
            addRequesterRecipient(message, purchaseRequest);
            if (PURCHASE_DENIED_ADDRESS != null) {
                addToRecipient(message, PURCHASE_DENIED_ADDRESS);
            }
            if (getRecipientCount(message) == 0) {
                return;
            }

            // Body
            String deniedReason = getDeniedReason(purchaseRequest);
            String text = buildText("denied", purchaseRequest, null, deniedReason);
            helper.setText(text, PURCHASE_DENIED_HTML);

            emailSender.send(message);
            log.info("Emailed denied purchase request: " + text);
        }
        catch (MessagingException e) {
            log.error("Failed to email new purchase request", e);
        }
    }

    private String getDeniedReason(PurchaseRequest purchaseRequest) {
        if (PURCHASE_DENIED_REASONS == null || 
            purchaseRequest.getDecisionReason() == null) { 
            return "";
        }
        String deniedReasonKey = purchaseRequest.getDecisionReason()
            .toLowerCase()
            .replaceAll(" ", "-");
        String deniedReason = PURCHASE_DENIED_REASONS.get(deniedReasonKey);
        return deniedReason == null ? "" : deniedReason;
    }

    @Override
    public void purchaseArrived(PurchaseRequest purchaseRequest) {
        log.debug("Email about purchase arrived.");
        email(new PurchaseArrivedEmailer(purchaseRequest.getKey()), PURCHASE_ARRIVED_DELAY);
    }

    private void emailPurchaseArrived(PurchaseRequest purchaseRequest) {
        try {            
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            prepStubMessage(helper);

            // Subject
            message.setSubject(buildSubject("Purchase Arrived"));

            // Recipients
            addRequesterRecipient(message, purchaseRequest);
            if (PURCHASE_ARRIVED_ADDRESS != null) {
                addToRecipient(message, PURCHASE_ARRIVED_ADDRESS);
            }
            if (getRecipientCount(message) == 0) {
                return;
            }

            // Body
            PurchasedItem purchasedItem = postPurchaseService.getFromRequest(purchaseRequest);
            String templateName = determineTemplate("arrived", purchasedItem);
            String text = buildText(templateName, purchaseRequest, purchasedItem, null);
            helper.setText(text, PURCHASE_ARRIVED_HTML);

            emailSender.send(message);
            log.info("Emailed arrived purchase request: " + text);
        }
        catch (MessagingException e) {
            log.error("Failed to email arrived purchase request", e);
        }
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

    private void prepStubMessage(MimeMessageHelper helper) throws MessagingException {
        helper.setFrom(FROM_ADDRESS);
    }

    private void addToRecipient(MimeMessage message, String address) throws MessagingException {
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(address));
    }

    private int getRecipientCount(MimeMessage message) throws MessagingException {
        Address[] toRecipients = message.getRecipients(Message.RecipientType.TO);
        return toRecipients == null ? 0 : toRecipients.length;
    }

    private String buildSubject(String subject) {
        if (SUBJECT_PREFIX != null) {
            subject = SUBJECT_PREFIX + subject;
        }
        return subject;
    }

    private void addLibrarianRecipient(MimeMessage message, PurchaseRequest purchaseRequest) 
        throws MessagingException {

        if (purchaseRequest.getLibrarianUsername() != null) {
            addToRecipient(message, purchaseRequest.getLibrarianUsername() + '@' + ADDRESS_DOMAIN);
        }
    }

    private void addRequesterRecipient(MimeMessage message, PurchaseRequest purchaseRequest) 
        throws MessagingException {
        
        if (purchaseRequest.getRequesterUsername() != null) {
            addToRecipient(message, purchaseRequest.getRequesterUsername() + '@' + ADDRESS_DOMAIN);
        }
    }

    private String buildText(String templateName, PurchaseRequest purchaseRequest) {
        return buildText(templateName, purchaseRequest, null, null);
    }

    private String buildText(String templateName, PurchaseRequest purchaseRequest, 
        PurchasedItem purchasedItem, String decisionReason) {

        log.debug("Using template " + templateName);
        Context context = new Context();
        context.setVariable("purchaseRequest", purchaseRequest);
        context.setVariable("decisionReason", decisionReason);
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
